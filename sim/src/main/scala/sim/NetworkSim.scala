package sim

import api.Servers
import cats.Parallel
import cats.effect.*
import cats.effect.kernel.Async
import cats.effect.std.{Console, Random}
import cats.syntax.all.*
import dproc.data.Block
import fs2.Stream
import io.rhonix.node.Node
import org.http4s.EntityEncoder
import rhonix.api.http.routes.All
import rhonix.execution.OnlyBalancesEngine.Deploy
import sdk.DagCausalQueue
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
  )

  // number of blocks to be produced by each sender
  val numBlocks = 50000

  /** Init simulation. Return list of streams representing processes of the computer. */
  def sim[F[_]: Async: Parallel: Random: Console](c: Config): Stream[F, Unit] = {

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
    }

    val x = mkNet(lfs)
      .map(_.zipWithIndex)
      .map { net =>
        net.map {
          case (self, Node(weaverStRef, processorStRef, proposerStRef, bufferStRef, dProc, api, saveBlock)) -> idx =>
            val bootstrap =
              Stream.eval(
                saveBlock(genesisM) *> dProc.acceptMsg(genesisM.id) >> Console[F].println(s"Bootstrap done for ${self}"),
              )
            val notSelf   = net.collect { case (x @ (s, _)) -> _ if s != self => s -> x._2 }

            val run = dProc.dProcStream concurrently {
              dProc.output
                .evalTap(x => api.blocks.get(x).map(b => blocks.put(x, Block.WithId(x, b))))
                .through(broadcast(notSelf, c.propDelay))
            }

            val tpsRef    = Ref.unsafe[F, Int](0)
            val tpsUpdate = dProc.finStream
              .through(TPS)
              .evalTap(x => tpsRef.set(x / c.size)) // finality is computed by each sender eventually
            val getData =
              (idx.pure, tpsRef.get, weaverStRef.get, proposerStRef.get, processorStRef.get, bufferStRef.get).tupled

            val apiServerStream: Stream[F, ExitCode] = if (idx == 0) {
              implicit val a: EntityEncoder[F, Long] = org.http4s.circe.jsonEncoderOf[F, Long]
              Servers.http[F](All[F, Long](api.balances), 8080, "localhost")
            } else Stream.empty

            (run concurrently bootstrap concurrently tpsUpdate concurrently apiServerStream) -> getData
        }
      }
      .map(_.unzip)
      .map { case (streams, diags) =>
        val simStream = Stream.emits(streams).parJoin(streams.size)

        simStream concurrently logDiag(diags.sequence)
      }

    Stream.force(x)
  }

  def logDiag[F[_]: Async: Console](
    getDiag: F[List[(Int, Int, WeaverState[M, S, T], Proposer.ST, Processor.ST[M], DagCausalQueue[M])]],
  ) = Stream
    .awakeEvery(150.millis)
    .evalMap(_ => getDiag)
    .map { x =>
      val str = x.sortBy(_._1).map { case (_, tps, weaver, proposer, processor, buffer) =>
        val processorData = s"${processor.processingSet.size} / ${processor.waitingList.size}(${processor.concurrency})"
        f"$tps%5s ${weaver.lazo.dagData.size}%10s ${proposer.status}%16s ${processorData}%20s ${buffer.dequeue._2.size}%12s"
      }
      s"""  BPS | Consensus size | Proposer status | Processor size | Buffer size
         |${str.mkString("\n")}
         |""".stripMargin
    }
    .evalTap { x =>
      print("\u001b[2J")
      Console[F].print(x)
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
        Random.scalaUtilRandom[IO].flatMap { implicit rndIO =>
          NetworkSim.sim[IO](config).compile.drain.as(ExitCode.Success)
        }

      case x => IO.println(s"Illegal option '${x.mkString(" ")}': see --help").as(ExitCode.Error)
    }
  }
}
