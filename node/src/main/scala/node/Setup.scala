package node

import cats.Parallel
import cats.data.ValidatedNel
import cats.effect.kernel.{Async, Resource, Sync}
import cats.effect.{ExitCode, Ref}
import cats.syntax.all.*
import diagnostics.metrics.InfluxDbBatchedMetrics
import dproc.data.Block
import fs2.Stream
import node.api.web
import node.api.web.{PublicApiJson, Validation}
import node.api.web.https4s.RouterFix
import node.balances.BalancesStateBuilderWithReader
import node.codec.Hashing.*
import node.codec.Serialization.balanceCodec
import org.http4s.HttpRoutes
import sdk.api.data.*
import sdk.api.{ApiErr, ExternalApi}
import sdk.codecs.Digest
import sdk.data.{BalancesDeploy, BalancesDeployBody, BalancesState}
import sdk.diag.Metrics
import sdk.history.ByteArray32
import sdk.primitive.ByteArray
import sdk.store.{HistoryWithValues, InMemoryKeyValueStoreManager}
import sdk.syntax.all.sdkSyntaxByteArray
import weaver.data.ConflictResolution

import java.nio.file.Path

/** Code to instantiate node components. */
object Setup {
  // Metrics reporter
  def metrics[F[_]: Async](
    enableInfluxDb: Boolean,
    influxDbConfig: diagnostics.metrics.Config,
    sourceTag: String,
  ): Resource[F, Metrics[F]] =
    if (enableInfluxDb) InfluxDbBatchedMetrics[F](influxDbConfig, sourceTag)
    else Resource.eval(Metrics.unit.pure[F])

  def mkDeployPool[F[_]: Sync]: (BalancesDeploy => F[Unit], ByteArray => F[Option[BalancesState]]) = {
    // Shared transactions store
    val txStore: Ref[F, Map[ByteArray, BalancesState]]  = Ref.unsafe(Map.empty[ByteArray, BalancesState])
    def saveTx(tx: BalancesDeploy): F[Unit]             = txStore.update(_.updated(tx.id, tx.body.state))
    def readTx(id: ByteArray): F[Option[BalancesState]] = txStore.get.map(_.get(id))
    saveTx -> readTx
  }

  def mkBlockStore[F[_]: Sync, M, S, T]: (
    Block.WithId[M, S, T] => F[Unit],
    M => F[Option[Block[M, S, T]]],
  ) = {
    val blockStore: Ref[F, Map[M, dproc.data.Block[M, S, T]]] =
      Ref.unsafe(Map.empty[M, dproc.data.Block[M, S, T]])
    def saveBlock(b: Block.WithId[M, S, T]): F[Unit]          =
      blockStore.update(_.updated(b.id, b.m))
    def readBlock(id: M): F[Option[Block[M, S, T]]]           = blockStore.get.map(_.get(id))

    (saveBlock, readBlock)
  }

  def lmdbBalancesShard[F[_]: Async: Parallel: Metrics](
    dataDir: Path,
  ): Resource[F, BalancesStateBuilderWithReader[F]] = Resource
//    .eval(LmdbStoreManager[F](dataDir))
    .eval(InMemoryKeyValueStoreManager[F]().pure)
    .flatMap(kvStoreManager => HistoryWithValues[F, Balance](kvStoreManager))
    .evalMap(BalancesStateBuilderWithReader(_))

  def webApi[F[_]: Async](
    getBlock: ByteArray => F[Option[Block[ByteArray, ByteArray, BalancesDeploy]]],
    host: String,
    port: Int,
    devMode: Boolean,
    saveTx: BalancesDeploy => F[Unit],
    readTx: ByteArray => F[Option[BalancesState]],
    readBalance: (ByteArray32, ByteArray) => F[Option[Balance]],
    getLatestM: F[Set[ByteArray]],
  ): Stream[F, ExitCode] = {
    def blockByHash(x: Array[Byte]): F[Option[sdk.api.data.Block]] =
      getBlock(ByteArray(x))
        .map(
          _.map { x =>
            x.copy(
              merge = x.merge.map(_.id),
              txs = x.txs.map(_.id),
              finalized = x.finalized.map { case ConflictResolution(accepted, rejected) =>
                ConflictResolution(accepted.map(_.id), rejected.map(_.id))
              },
            )
          }.map {
            case Block(
                  sender,
                  minGenJs,
                  offences,
                  txs,
                  finalFringe,
                  finalized,
                  merge,
                  bonds,
                  lazTol,
                  expThresh,
                  finalStateHash,
                  postStateHash,
                ) =>
              sdk.api.data.Block(
                x,
                sender.bytes,
                1,
                "root",
                -1,
                -1,
                minGenJs.map(_.bytes),
                bonds.bonds.map { case (k, v) => Bond(k.bytes, v) }.toSet,
                finalStateHash,
                preStateHash = postStateHash,
                postStateHash = postStateHash,
                deploys = txs.map(_.bytes).toSet,
                signatureAlg = "-",
                signature = Array.empty[Byte],
                status = 0,
              )
          },
        )

    val routes: HttpRoutes[F] = {
      val externalApi = new ExternalApi[F] {
        override def getBlockByHash(hash: Array[Byte]): F[Option[sdk.api.data.Block]] = blockByHash(hash)

        override def getDeployByHash(hash: Array[Byte]): F[Option[Deploy]] =
          readTx(ByteArray(hash)).map(
            _.map(x =>
              Deploy(
                Array.empty[Byte],
                Array.empty[Byte],
                "root",
                s"${x.diffs.map { case k -> v => k.toHex -> v }}",
                0L,
                0L,
                0L,
                0L,
                0L,
              ),
            ),
          )

        override def getDeploysByHash(hash: Array[Byte]): F[Option[Seq[Array[Byte]]]] = ???

        override def getBalance(state: Array[Byte], wallet: Array[Byte]): F[Option[Long]] = {
          val blakeH = ByteArray32.convert(state)
          blakeH.map(_ -> ByteArray(wallet)).traverse(readBalance.tupled(_).map(_.map(_.x))).flatMap(_.liftTo[F])
        }

        override def getLatestMessages: F[List[Array[Byte]]] =
          getLatestM.map(_.toList.map(_.bytes.bytes.bytes))

        override def status: F[Status] = Status("0.1.1").pure

        override def transferToken(tx: TokenTransferRequest): F[ValidatedNel[ApiErr, Unit]] =
          Validation
            .validateTokenTransferRequest(tx)(implicitly[Digest[TokenTransferRequest.Body]])
            .map(_.body)
            .traverse { case TokenTransferRequest.Body(sender, recipient, _, amount, vafn) =>
              saveTx(
                new BalancesDeploy(
                  ByteArray(sender),
                  BalancesDeployBody(
                    BalancesState(Map(ByteArray(sender) -> -amount, ByteArray(recipient) -> amount)),
                    vafn,
                  ),
                ),
              )
            }
      }

      PublicApiJson[F](externalApi).routes
    }

    val allRoutes = RouterFix(s"/${sdk.api.RootPath.mkString("/")}" -> routes)

    web.server(allRoutes, port, host, devMode)
  }
}
