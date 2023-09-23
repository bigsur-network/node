package sim

import cats.Parallel
import cats.effect.*
import cats.effect.kernel.{Async, Temporal}
import cats.effect.std.{Console, Random}
import cats.syntax.all.*
import dproc.data.Block
import fs2.{Pipe, Stream}
import io.circe.Encoder
import io.rhonix.node.Node
import io.rhonix.node.api.http
import io.rhonix.node.api.http.routes.*
import org.http4s.EntityEncoder
import rhonix.diagnostics.KamonContextStore
import sdk.api.*
import sdk.codecs.Base16
import sdk.hashing.Blake2b256Hash
import sdk.history.History.EmptyRootHash
import sdk.store.{ByteArrayKeyValueTypedStore, InMemoryKeyValueStore}
import sdk.syntax.all.*
import sim.balances.*
import sim.balances.MergeLogicForPayments.mergeRejectNegativeOverflow
import sim.balances.data.BalancesState.Default
import sim.balances.data.{BalancesDeploy, BalancesState}
import weaver.WeaverState
import weaver.data.*

import scala.concurrent.duration.{Duration, DurationInt, MICROSECONDS}
import scala.util.Try

object NetworkSim extends IOApp {

  /// Users (wallets) making transactions
  val users: Set[Wallet] = (1 to 100).toSet

  // Dummy types for message id, sender id and transaction
  type M = String
  type S = String
  type T = BalancesDeploy
  implicit val ordS = new Ordering[String] {
    override def compare(x: S, y: S): Int = x compareTo y
  }

  final case class Config(
    size: Int,
    processingConcurrency: Int,
    exeDelay: Duration,
    hashDelay: Duration,
    propDelay: Duration,
    rcvDelay: Duration,
    stateReadTime: Duration,
    // TODO for partially sync consensus
    //    lazinessTolerance: Int,
  )

  final case class NetNode[F[_]](
    id: S,
    node: Node[F, M, S, T],
    balanceApi: (Blake2b256Hash, Wallet) => F[Option[Long]],
    historyStore: InMemoryKeyValueStore[F],
    fringeMapping: Set[M] => F[Blake2b256Hash],
  )

  def genesisBlock[F[_]: Async: Parallel](sender: S, genesisExec: FinalData[S]): F[Block.WithId[M, S, T]] = {
    val mkHistory     = sdk.history.History.create(EmptyRootHash, new InMemoryKeyValueStore[F])
    val mkValuesStore = Sync[F].delay {
      new ByteArrayKeyValueTypedStore[F, Blake2b256Hash, Balance](
        new InMemoryKeyValueStore[F],
        Blake2b256Hash.codec,
        balanceCodec,
      )
    }

    (mkHistory, mkValuesStore).flatMapN { case history -> valueStore =>
      val genesisState  = new BalancesState(users.map(_ -> Long.MaxValue / 2).toMap)
      val genesisDeploy = BalancesDeploy("genesis", genesisState)
      BalancesStateBuilderWithReader(history, valueStore)
        .buildState(
          baseState = EmptyRootHash,
          toFinalize = Default,
          toMerge = genesisState,
        )
        .map { case _ -> postState =>
          Block.WithId(
            s"genesis",
            Block[M, S, T](
              sender,
              Set(),
              Set(),
              txs = List(genesisDeploy),
              Set(),
              None,
              Set(),
              genesisExec.bonds,
              genesisExec.lazinessTolerance,
              genesisExec.expirationThreshold,
              finalStateHash = EmptyRootHash,
              postStateHash = postState,
            ),
          )
        }
    }
  }

  def sim[F[_]: Async: Parallel: Random: Console: KamonContextStore](c: Config): Stream[F, Unit] = {

    /// Genesis data
    val lazinessTolerance = 1 // c.lazinessTolerance
    val senders           = Iterator.range(0, c.size).map(n => s"s$n").toList
    // Create lfs message, it has no parents, sees no offences and final fringe is empty set
    val genesisBonds      = Bonds(senders.map(_ -> 100L).toMap)
    val genesisExec       = FinalData(genesisBonds, lazinessTolerance, 10000)
    val lfs               = MessageData[M, S]("s0", Set(), Set(), FringeData(Set()), genesisExec)

    /// Shared block store across simulation
    // TODO replace with pgSql
    val blockStore: Ref[F, Map[M, Block[M, S, T]]] = Ref.unsafe(Map.empty[M, Block[M, S, T]])

    def saveBlock(b: Block.WithId[M, S, T]): F[Unit] = blockStore.update(_.updated(b.id, b.m))

    def readBlock(id: M): F[Block[M, S, T]] = blockStore.get.map(_.getUnsafe(id))

    def broadcast(
      peers: List[Node[F, M, S, T]],
      time: Duration,
    ): Pipe[F, M, Unit] = _.evalMap(m => Temporal[F].sleep(time) *> peers.traverse(_.dProc.acceptMsg(m)).void)

    def random(users: Set[Wallet]): F[BalancesState] = for {
      txVal <- Random[F].nextLongBounded(100)
      from  <- Random[F].elementOf(users)
      to    <- Random[F].elementOf(users - from)
    } yield new BalancesState(Map(from -> -txVal, to -> txVal))

    def mkNode(vId: S): F[NetNode[F]] = Sync[F].defer {
      val blockSeqNumRef = Ref.unsafe(0)
      val assignBlockId  = (_: Any) => blockSeqNumRef.updateAndGet(_ + 1).map(idx => s"$vId-$idx")

      val txSeqNumRef = Ref.unsafe(0)
      val nextTxs     = txSeqNumRef.updateAndGet(_ + 1).flatMap { idx =>
        random(users).map(st => Set(balances.data.BalancesDeploy(s"$vId-tx-$idx", st)))
      }

      val historyStore  = new InMemoryKeyValueStore[F]
      val mkHistory     = sdk.history.History.create(EmptyRootHash, historyStore)
      val mkValuesStore = Sync[F].delay {
        new ByteArrayKeyValueTypedStore[F, Blake2b256Hash, Balance](
          new InMemoryKeyValueStore[F],
          Blake2b256Hash.codec,
          balanceCodec,
        )
      }

      (mkHistory, mkValuesStore).flatMapN { case history -> valueStore =>
        val balancesEngine   = BalancesStateBuilderWithReader(history, valueStore)
        val fringeMappingRef = Ref.unsafe(Map(Set.empty[M] -> EmptyRootHash))

        def buildState(
          baseFringe: Set[M],
          finalFringe: Set[M],
          toFinalize: Set[T],
          toMerge: Set[T],
          toExecute: Set[T],
        ): F[((Blake2b256Hash, Seq[T]), (Blake2b256Hash, Seq[T]))] =
          for {
            baseState <- fringeMappingRef.get.map(_(baseFringe))
            r         <- mergeRejectNegativeOverflow(balancesEngine, baseState, toFinalize, toMerge ++ toExecute)

            ((newFinState, finRj), (newMergeState, provRj)) = r

            r <- balancesEngine.buildState(baseState, newFinState, newMergeState)

            (finalHash, postHash) = r

            _ <- fringeMappingRef.update(_ + (finalFringe -> finalHash))
          } yield ((finalHash, finRj), (postHash, provRj))

        Node[F, M, S, T](
          vId,
          WeaverState.empty[M, S, T](lfs.state),
          assignBlockId,
          nextTxs,
          buildState,
          saveBlock,
          readBlock,
        ).map(
          NetNode(
            vId,
            _,
            balancesEngine.readBalance(_: Blake2b256Hash, _: Wallet),
            historyStore,
            (x: Set[M]) => fringeMappingRef.get.map(_.getUnsafe(x)),
          ),
        )
      }
    }

    /** Make the computer, init all peers with lfs. */
    def mkNet(lfs: MessageData[M, S]): F[List[NetNode[F]]] = lfs.state.bonds.activeSet.toList.traverse(mkNode)

    val x: F[Stream[F, Unit]] = mkNet(lfs)
      .map(_.zipWithIndex)
      .map { net =>
        net.map {
          case NetNode(
                self,
                Node(weaverStRef, processorStRef, proposerStRef, bufferStRef, dProc),
                getBalance,
                historyStore,
                fringeToHash,
              ) -> idx =>
            val bootstrap =
              Stream.eval(genesisBlock[F](senders.head, genesisExec).flatMap { genesisM =>
                saveBlock(genesisM) *> dProc.acceptMsg(genesisM.id) >> Console[F].println(s"Bootstrap done for ${self}")
              })
            val notSelf   = net.collect { case NetNode(id, node, _, _, _) -> _ if id != self => node }

            val run = dProc.dProcStream concurrently {
              dProc.output.through(broadcast(notSelf, c.propDelay))
            }

            val tpsRef    = Ref.unsafe[F, Float](0f)
            val tpsUpdate = dProc.finStream
              .map(_.accepted.toList)
              .flatMap(Stream.emits(_))
              .throughput(1.second)
              // finality is computed by each sender eventually so / c.size
              .evalTap(x => tpsRef.set(x.toFloat / c.size))
            val getData   =
              (
                idx.pure,
                tpsRef.get,
                weaverStRef.get,
                proposerStRef.get,
                processorStRef.get,
                bufferStRef.get,
              ).flatMapN { case (id, tps, w, p, pe, b) =>
                val lfsHashF =
                  fringeToHash(
                    w.lazo.fringes.minByOption { case (i, _) => i }.map { case (_, fringe) => fringe }.getOrElse(Set()),
                  )
                lfsHashF.map(NetworkSnapshot.NodeSnapshot(id, tps, w, p, pe, b, historyStore.State.size, _))
              }

            val apiServerStream: Stream[F, ExitCode] = {
              import io.circe.generic.auto.*
              import io.circe.generic.semiauto.*
              import org.http4s.circe.*

              implicit val c: String => Try[Int]            = (x: String) => Try(x.toInt)
              implicit val d: String => Try[String]         = (x: String) => Try(x)
              implicit val e: String => Try[Blake2b256Hash] =
                (x: String) => Base16.decode(x).flatMap(Blake2b256Hash.deserialize)
              implicit val encoder: Encoder[Blake2b256Hash] =
                Encoder[String].imap(s => Blake2b256Hash(Base16.decode(s).getUnsafe))(x => Base16.encode(x.bytes.bytes))

              implicit val a: EntityEncoder[F, Long]           = jsonEncoderOf[F, Long]
              implicit val x: EntityEncoder[F, Int]            = jsonEncoderOf[F, Int]
              implicit val f: EntityEncoder[F, String]         = jsonEncoderOf[F, String]
              implicit val g: EntityEncoder[F, Blake2b256Hash] = jsonEncoderOf[F, Blake2b256Hash]
              implicit val h1: EntityEncoder[F, Set[String]]   = jsonEncoderOf[F, Set[String]]

              implicit val h: EntityEncoder[F, Block[String, String, String]] =
                jsonEncoderOf[F, Block[String, String, String]]

              def blockByHash(x: M): F[Option[Block[M, S, String]]] =
                blockStore.get
                  .map(_.get(x))
                  .map(
                    _.map(x =>
                      x.copy(
                        merge = x.merge.map(_.id),
                        txs = x.txs.map(_.id),
                        finalized = x.finalized.map { case ConflictResolution(accepted, rejected) =>
                          ConflictResolution(accepted.map(_.id), rejected.map(_.id))
                        },
                      ),
                    ),
                  )

              def latestBlocks: F[Set[M]] = weaverStRef.get.map(_.lazo.latestMessages)

              val routes =
                HttpGet[F, Set[M]]("latest", latestBlocks.map(_.some)) <+>
                  HttpGet[F, Blake2b256Hash, Wallet, Balance](BalancesApi.MethodName, getBalance) <+>
                  HttpGet[F, M, Block[M, S, String]](BlockDbApi.MethodName, blockByHash)

              http.server(routes, 8080 + idx, "localhost")
            }

            (run concurrently bootstrap concurrently tpsUpdate concurrently apiServerStream) -> getData
        }
      }
      .map(_.unzip)
      .map { case (streams, diags) =>
        val simStream = Stream.emits(streams).parJoin(streams.size)

        val logDiag = {
          val getNetworkState = diags.sequence
          import NetworkSnapshot.*
          getNetworkState.showAnimated(samplingTime = 1.second)
        }

        simStream concurrently logDiag
      }

    Stream.force(x)
  }

  override def run(args: List[String]): IO[ExitCode] = {
    val prompt = """
    This uberjar simulates the network of nodes running block merge with synchronous consensus.
    Execution engine (rholang) and the network conditions are abstracted away but their behaviour can be configurable.

    Usage: specify 8 input arguments:
     1. Number of nodes in the network.
     2. Number of blocks that node is allowed to process concurrently.
     3. Time to execute block (microseconds).
     4. Time to hash and sign block (microseconds).
     5. Network propagation delay (microseconds).
     6. Time to download full block having hash (microseconds).
     7. Rholang state read time (microseconds).
     8. Laziness tolerance (number of fringes to keep) To get the fastest result keep it 0.

     eg java -jar *.jar 16 16 0 0 0 0 0 0

    The output of this binary is the data read from each nodes state every 150ms and is formatted as follows:
      BPS - blocks finalized by the node per second.
      Consensus size - number of blocks required to run consensus (with some leeway set by laziness tolerance).
      Proposer status - status of the block proposer.
      Processor size - number of blocks currently in processing / waiting for processing.
      Buffer size - number of blocks in the buffer.
    """.stripMargin

    args match {
      case List("--help") => IO.println(prompt).as(ExitCode.Success)
      case List(
            size,
            processingConcurrency,
            exeDelay,
            hashDelay,
            propDelay,
            rcvDelay,
            stateReadTime,
            lazinessTolerance,
          ) =>
        val config = Config(
          size.toInt,
          processingConcurrency.toInt,
          Duration(exeDelay.toLong, MICROSECONDS),
          Duration(hashDelay.toLong, MICROSECONDS),
          Duration(propDelay.toLong, MICROSECONDS),
          Duration(rcvDelay.toLong, MICROSECONDS),
          Duration(stateReadTime.toLong, MICROSECONDS),
//          lazinessTolerance.toInt,
        )

        implicit val kts: KamonContextStore[IO] = KamonContextStore.forCatsEffectIOLocal
        Random.scalaUtilRandom[IO].flatMap { implicit rndIO =>
          NetworkSim.sim[IO](config).compile.drain.as(ExitCode.Success)
        }

      case x => IO.println(s"Illegal option '${x.mkString(" ")}': see --help").as(ExitCode.Error)
    }
  }
}
