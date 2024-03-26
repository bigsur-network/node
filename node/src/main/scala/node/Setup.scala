package node

import cats.Parallel
import cats.effect.kernel.Ref.Make
import cats.effect.{Async, Ref, Resource, Sync}
import cats.syntax.all.*
import diagnostics.metrics.InfluxDbBatchedMetrics
import dproc.DProc
import dproc.DProc.ExeEngine
import dproc.data.Block
import node.Codecs.*
import node.Hashing.*
import node.Node.BalancesShardName
import node.api.web.PublicApiJson
import node.api.web.https4s.RouterFix
import node.api.{web, ExternalApiSlickImpl}
import node.comm.PeerTable
import node.lmdb.LmdbStoreManager
import node.rpc.syntax.all.grpcClientSyntax
import node.rpc.{GrpcChannelsManager, GrpcClient, GrpcServer}
import sdk.serialize.auto.*
import node.Hashing.*
import node.state.StateManager
import org.http4s.HttpRoutes
import org.http4s.server.Server
import sdk.api.ExternalApi
import sdk.api.data.Balance
import sdk.comm.Peer
import sdk.data.{BalancesDeploy, HostWithPort}
import sdk.diag.Metrics
import sdk.hashing.Blake2b
import sdk.history.History
import sdk.log.Logger.*
import sdk.merging.MergeLogicForPayments.mergeRejectNegativeOverflow
import sdk.merging.Relation
import sdk.primitive.ByteArray
import sdk.store.{HistoryWithValues, InMemoryKeyValueStoreManager, KeyValueStoreManager, KeyValueTypedStore}
import sdk.syntax.all.*
import slick.SlickDb
import slick.api.SlickApi
import slick.jdbc.JdbcBackend.DatabaseDef
import slick.jdbc.PostgresProfile
import slick.migration.api.PostgresDialect
import weaver.WeaverState
import weaver.data.FinalData

import java.nio.file.Path

/** Node setup. */
final case class Setup[F[_]](
  // sql database
  database: SlickApi[F],
  // key value store managers
  diskKvStoreManager: KeyValueStoreManager[F],
  memKvStoreManager: KeyValueStoreManager[F],
  // web server
  webServer: org.http4s.server.Server,
  // grpc server
  grpcServer: io.grpc.Server,
  // metrics streamer
  metrics: Metrics[F],
  // peerTable
  peerManager: PeerTable[F, (String, Int), Peer],
  grpcChannelsManager: GrpcChannelsManager[F],
  blockResolver: BlockResolver[F],
  // shard for balances
  balancesShard: BalancesStateBuilderWithReader[F],
  // deploy pool
  deployPool: KeyValueTypedStore[F, ByteArray, BalancesDeploy],
  stateManager: StateManager[F],
  dProc: DProc[F, ByteArray, BalancesDeploy],
  nodeStream: fs2.Stream[F, Unit],
)

object Setup {
  private def database[F[_]: Async](dbDef: Resource[F, DatabaseDef]): Resource[F, SlickApi[F]] =
    dbDef.evalMap(x => SlickDb(x, PostgresProfile, new PostgresDialect).flatMap(SlickApi[F]))

  private def kvDiskStoreManager[F[_]: Async](dataDir: Path): Resource[F, KeyValueStoreManager[F]] =
    Resource.make(LmdbStoreManager[F](dataDir))(_.shutdown)

  private def kvMemStoreManager[F[_]: Async]: Resource[F, KeyValueStoreManager[F]] =
    Resource.make(Sync[F].delay(InMemoryKeyValueStoreManager[F]()))(_.shutdown)

  private def metrics[F[_]: Async](
    enableInfluxDb: Boolean,
    influxDbConfig: diagnostics.metrics.Config,
    sourceTag: String,
  ): Resource[F, Metrics[F]] =
    if (enableInfluxDb) InfluxDbBatchedMetrics[F](influxDbConfig, sourceTag)
    else Resource.pure(Metrics.unit)

  private def webServer[F[_]: Async](
    host: String,
    port: Int,
    devMode: Boolean,
    externalApi: ExternalApi[F],
  ): Resource[F, Server] = {
    val routes: HttpRoutes[F] = PublicApiJson[F](externalApi).routes
    val allRoutes             = RouterFix(s"/${sdk.api.RootPath.mkString("/")}" -> routes)
    web.server(allRoutes, port, host, devMode)
  }

  private def dProc[F[_]: Async: Parallel: Metrics](
    id: ByteArray,
    state: StateManager[F],
    balancesShard: BalancesStateBuilderWithReader[F],
    genesisPoS: FinalData[ByteArray],
    database: SlickApi[F],
    deploysPooled: F[Set[BalancesDeploy]],
  ): Resource[F, DProc[F, ByteArray, BalancesDeploy]] = Resource.eval(Sync[F].defer {
    val exeEngine = new ExeEngine[F, ByteArray, ByteArray, BalancesDeploy] {
      def execute(
        base: Set[ByteArray],
        fringe: Set[ByteArray],
        toFinalize: Set[BalancesDeploy],
        toMerge: Set[BalancesDeploy],
        txs: Set[BalancesDeploy],
      ): F[((ByteArray, Seq[BalancesDeploy]), (ByteArray, Seq[BalancesDeploy]))] =
        for {
          baseState <- state.fringeMappingRef.get.map(_.getOrElse(base, History.EmptyRootHash))

          r <- mergeRejectNegativeOverflow(
                 balancesShard.readBalance,
                 baseState,
                 toFinalize,
                 toMerge ++ txs,
               )

          ((newFinState, finRj), (newMergeState, provRj)) = r

          r <- balancesShard.buildState(baseState, newFinState, newMergeState)

          (finalHash, postHash) = r

          _ <- state.fringeMappingRef.update(_ + (fringe -> finalHash)).unlessA(fringe.isEmpty)
        } yield ((finalHash.bytes, finRj), (postHash.bytes, provRj))

      // data read from the final state associated with the final fringe
      def consensusData(fringe: Set[ByteArray]): F[FinalData[ByteArray]] =
        genesisPoS.pure[F] // TODO for now PoS is static defined in genesis
    }

    val dbApiImpl = DbApiImpl(database)
    val saveBlock = dbApiImpl.saveBlock _
    val readBlock = dbApiImpl.readBlock(_: ByteArray).flatMap(_.liftTo[F](new Exception("Block not found")))

    DProc
      .apply[F, ByteArray, ByteArray, BalancesDeploy](
        state.weaverStRef,
        state.propStRef,
        state.procStRef,
        state.bufferStRef,
        deploysPooled,
        id.some,
        exeEngine,
        Relation.notRelated[F, BalancesDeploy],
        b => Sync[F].delay(ByteArray(Blake2b.hash256(b.digest.bytes))),
        saveBlock,
        readBlock,
      )
  })

  private def deployPool[F[_]: Sync: Make]: Resource[F, KeyValueTypedStore[F, ByteArray, BalancesDeploy]] =
    Resource.eval(Ref[F].of(Map.empty[ByteArray, BalancesDeploy]).map { st =>
      new KeyValueTypedStore[F, ByteArray, BalancesDeploy] {
        override def get(keys: Seq[ByteArray]): F[Seq[Option[BalancesDeploy]]] =
          st.get.map(_.view.filterKeys(keys.contains).values.toSeq.map(Option.apply))

        override def put(kvPairs: Seq[(ByteArray, BalancesDeploy)]): F[Unit] = st.update(_.++(kvPairs))

        override def delete(keys: Seq[ByteArray]): F[Int] = st.update(_.--(keys)).as(0)

        override def contains(keys: Seq[ByteArray]): F[Seq[Boolean]] =
          st.get.map(_.view.map(keys.contains).toSeq)

        override def collect[T](pf: PartialFunction[(ByteArray, () => BalancesDeploy), T]): F[Seq[T]] = ???

        override def toMap: F[Map[ByteArray, BalancesDeploy]] = st.get
      }
    })

  def all[F[_]: Async: Parallel](
    dbDef: Resource[F, DatabaseDef],
    id: ByteArray,
    // TODO for now node always starts from the same genesis state, specified either in simulator or in main function
    //   make it restored from the database
    genesisPoS: FinalData[ByteArray],
    dummyDeploys: F[Set[BalancesDeploy]],
    idx: Int = 0,
    selfFQDN: String = "localhost",
  ): Resource[F, Setup[F]] = for {
    _                 <- Resource.eval(logDebugF(s"Node setup started at address $selfFQDN."))
    // connect to the database
    database          <- database[F](dbDef)
    // load node configuration
    cfg               <- node.ConfigManager.buildConfig[F](database)
    (nCfg, mCfg, cCfg) = cfg
    // metrics
    metrics           <- metrics(nCfg.enableInfluxDb, mCfg, id.toHex)
    // kv store managers (for rholang execution)
    diskKvManager     <- kvDiskStoreManager(nCfg.kvStoresPath)
    memKvManager      <- kvMemStoreManager
    // shard for balances
    balanceHwV        <- Resource.eval(HistoryWithValues[F, Balance](BalancesShardName, diskKvManager))
    balancesShard      = { implicit val m: Metrics[F] = metrics; BalancesStateBuilderWithReader(balanceHwV) }
    // deploy pool
    dPool             <- deployPool
    // state
    nodeState         <- Resource.eval(StateManager[F](WeaverState.empty[ByteArray, ByteArray, BalancesDeploy](genesisPoS)))
    // api
    latestM            = nodeState.weaverStRef.get.map(_.lazo.latestMessages.toList)
    extApiImpl         = ExternalApiSlickImpl(database, balancesShard, latestM, dPool)
    // web server
    webServer         <- webServer[F](nCfg.httpHost, nCfg.httpPort + idx, nCfg.devMode, extApiImpl)
    // grpc server
    grpcChManager     <- GrpcChannelsManager[F]
    blockResolver     <- {
      implicit val g: GrpcChannelsManager[F] = grpcChManager
      Resource.eval(BlockResolver.apply[F])
    }
    // resolve received hash to block and send to validation
    hashCallback       = (h: ByteArray, s: HostWithPort) =>
                           DbApiImpl(database)
                             .isBlockExist(h)
                             .ifM(false.pure, blockResolver.in(h, s).as(true))
    blockReqCallback   = (h: ByteArray) => DbApiImpl(database).readBlock(h).map(_.map(b => Block.WithId(h, b)))
    grpcSrv           <- GrpcServer.apply[F](nCfg.gRpcPort + idx, hashCallback, blockReqCallback, _ => latestM.map(_.toSeq))
    // peerTable
    peerTable         <- { implicit val db: SlickApi[F] = database; Resource.eval(PeerTable(cCfg)) }
    // core logic
    dProc             <- {
      implicit val m: Metrics[F] = metrics
      val deploysWithDummy       = (dPool.toMap.map(_.values.toSet), dummyDeploys).mapN(_ ++ _)
      dProc(id, nodeState, balancesShard, genesisPoS, database, deploysWithDummy)
    }
    _                 <- Resource.eval(logDebugF(s"Node setup complete."))
  } yield {
    val pullBlocksFromNetwork = blockResolver.out
      .evalTap(DbApiImpl(database).saveBlock)
      .map(_.id)
      .evalTap(dProc.acceptMsg)

    val broadcastOutput = {
      implicit val chm: GrpcChannelsManager[F]           = grpcChManager
      implicit val pt: PeerTable[F, (String, Int), Peer] = peerTable
      dProc.output
        .evalMap(h => GrpcClient[F].broadcastBlockHash(h, HostWithPort(selfFQDN, nCfg.gRpcPort + idx)))
    }

    val nodeStream = dProc.dProcStream concurrently pullBlocksFromNetwork concurrently broadcastOutput

    Setup(
      database,
      diskKvManager,
      memKvManager,
      webServer,
      grpcSrv,
      metrics,
      peerTable,
      grpcChManager,
      blockResolver,
      balancesShard,
      dPool,
      nodeState,
      dProc,
      nodeStream,
    )
  }
}
