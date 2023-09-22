package sim

import cats.Parallel
import cats.effect.*
import cats.effect.kernel.Async
import cats.effect.std.{Console, Random}
import cats.syntax.all.*
import dproc.data.Block
import fs2.Stream
import io.rhonix.node.Node
import io.rhonix.node.api.http
import io.rhonix.node.api.http.routes.All
import org.http4s.EntityEncoder
import rhonix.execution.OnlyBalancesEngine.Deploy
import sdk.DagCausalQueue
import sdk.api.*
import sdk.api.data.{Block as ApiBlock, BlockDeploys}
import sdk.node.{Processor, Proposer}
import sdk.syntax.all.*
import sim.Env.*
import weaver.WeaverState
import weaver.data.*

import scala.concurrent.duration.{Duration, DurationInt, MICROSECONDS}

object NetworkSim extends IOApp {
  // Dummy types for message id, sender id and transaction
  type M = String
  type S = String
  type T = String
  implicit val ordS = new Ordering[String] {
    override def compare(x: S, y: S): Int = x.length compareTo y.length
  }

  final case class Config(
    size: Int,
    processingConcurrency: Int,
    exeDelay: Duration,
    hashDelay: Duration,
    propDelay: Duration,
    rcvDelay: Duration,
    stateReadTime: Duration,
    lazinessTolerance: Int,

  final case class NetNode[F[_]](
    id: S,
    node: Node[F, M, S, T],
    balanceApi: (Blake2b256Hash, Wallet) => F[Long],
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

  /** Init simulation. Return list of streams representing processes of the computer. */
  def sim[F[_]: Async: Parallel: Random: Console: KamonContextStore](c: Config): Stream[F, Unit] = {

    /** Make the computer, init all peers with lfs. */
    def mkNet(lfs: MessageData[M, S]): F[List[(S, Node[F, M, S, T])]] =
      lfs.state.bonds.activeSet.toList.traverse { vId =>
        for {
          idsRef <- Ref.of(dummyIds(vId).take(numBlocks).toList)
          hasher  = (_: Any) =>
                      Async[F].sleep(c.hashDelay) >> idsRef.modify {
                        case head :: tail => (tail, head)
                        case _            => sys.error("No ids left")
                      }
          txMap   = (_: String) => Deploy(Map("s0" -> 1L, "s1" -> -1L))
          r      <-
            Node[F, M, S, T](
              vId,
              WeaverState.empty[M, S, T](lfs.state),
              lfs.state,
              hasher,
              c.exeDelay,
              c.stateReadTime,
              txMap,
              idsRef,
            )
              .map(vId -> _)
        } yield r
    /// Shared block store across simulation
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

      val blockSeqNumRef = Ref.unsafe(0)
      val assignBlockId  = (_: Any) => blockSeqNumRef.updateAndGet(_ + 1).map(idx => s"$vId-$idx")

      val txSeqNumRef = Ref.unsafe(0)
      val nextTxs     = txSeqNumRef.updateAndGet(_ + 1).flatMap { idx =>
        random(users).map(st => Set(balances.data.BalancesDeploy(s"$vId-tx-$idx", st)))
      }

    val senders      = Iterator.range(0, c.size).map(n => s"s#$n").toList
    // Create lfs message, it has no parents, sees no offences and final fringe is empty set
    val genesisBonds = Bonds(senders.map(_ -> 100L).toMap)
    val genesisExec  = FinalData(genesisBonds, c.lazinessTolerance, 10000)
    val lfs          = MessageData[M, S]("s#0", Set(), Set(), FringeData(Set()), genesisExec)
    val genesisM     = {
      val genesisTx  = List.empty[T]
      val genesisFin = ConflictResolution[T](genesisTx.toSet, Set()).some
      Block.WithId(
        s"0@${senders.head}",
        Block[M, S, T](
          senders.head,
          Set(),
          Set(),
          genesisTx,
          Set(),
          genesisFin,
          Set(),
          genesisExec.bonds,
          genesisExec.lazinessTolerance,
          genesisExec.expirationThreshold,
        ),
      )
        Node[F, M, S, T](
          vId,
          WeaverState.empty[M, S, T](lfs.state),
          assignBlockId,
          nextTxs,
          buildState,
          saveBlock,
          readBlock,
        ).map(NetNode(vId, _, balancesEngine.readBalance(_: Blake2b256Hash, _: Wallet).map(_.getOrElse(Long.MinValue))))
      }
    }

    val x = mkNet(lfs)
      .map(_.zipWithIndex)
      .map { net =>
        net.map {
          case NetNode(
                self,
                Node(weaverStRef, processorStRef, proposerStRef, bufferStRef, dProc),
                balancesApi,
              ) -> idx =>
            val bootstrap =
              Stream.eval(
                saveBlock(genesisM) *> dProc.acceptMsg(genesisM.id) >> Console[F].println(s"Bootstrap done for ${self}"),
              )
            val notSelf   = net.collect { case (x @ (s, _)) -> _ if s != self => s -> x._2 }

            val run = dProc.dProcStream concurrently {
              dProc.output.through(broadcast(notSelf, c.propDelay))
            }

            val tpsRef    = Ref.unsafe[F, Float](0f)
            val tpsUpdate = dProc.finStream
              .map(_.accepted)
              .throughput(1.second)
              // finality is computed by each sender eventually so / c.size
              .evalTap(x => tpsRef.set(x.toFloat / c.size))
            val getData =
            (idx.pure, tpsRef.get, weaverStRef.get, proposerStRef.get, processorStRef.get, bufferStRef.get).mapN(
              NetworkSnapshot.NodeSnapshot(_, _, _, _, _, _),
            )

            val apiServerStream: Stream[F, ExitCode] = if (idx == 0) {
              implicit val a: EntityEncoder[F, Long] = org.http4s.circe.jsonEncoderOf[F, Long]

              val dummyBlockDBApi   = new BlockDbApi[F] {
                override def insert(block: ApiBlock, senderId: Long): F[Long]           = 1L.pure[F]
                override def update(id: Long, block: ApiBlock, senderId: Long): F[Unit] = ().pure[F]
                override def getById(id: Long): F[Option[ApiBlock]]                     = none[ApiBlock].pure[F]
                override def getByHash(hash: Array[Byte]): F[Option[ApiBlock]]          = none[ApiBlock].pure[F]
              }
              val dummyDeploysDbApi = new BlockDeploysDbApi[F] {
                override def insert(blockDeploys: BlockDeploys): F[Unit]     = ().pure[F]
                override def getByBlock(blockId: Long): F[Seq[BlockDeploys]] = Seq.empty[BlockDeploys].pure[F]
              }
              val routes            = All[F, Long](dummyBlockDBApi, dummyDeploysDbApi, api.balances)
              http.server(routes, 8080, "localhost")
            } else Stream.empty

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
          lazinessTolerance.toInt,
        )

        implicit val kts: KamonContextStore[IO] = KamonContextStore.forCatsEffectIOLocal
        Random.scalaUtilRandom[IO].flatMap { implicit rndIO =>
          NetworkSim.sim[IO](config).compile.drain.as(ExitCode.Success)
        }

      case x => IO.println(s"Illegal option '${x.mkString(" ")}': see --help").as(ExitCode.Error)
    }
  }
}
