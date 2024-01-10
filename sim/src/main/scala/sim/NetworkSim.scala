package sim

import cats.Parallel
import cats.effect.*
import cats.effect.kernel.{Async, Temporal}
import cats.effect.std.{Console, Random}
import cats.syntax.all.*
import diagnostics.KamonContextStore
import diagnostics.metrics.Config as InfluxDbConfig
import dproc.data.Block
import fs2.concurrent.SignallingRef
import fs2.{Pipe, Stream}
import node.Node.State
import node.balances.Genesis
import node.codec.Hashing.*
import node.{Config as NodeConfig, Node, Setup}
import pureconfig.generic.ProductHint
import sdk.api.data.Balance
import sdk.data.{BalancesDeploy, BalancesDeployBody, BalancesState}
import sdk.diag.{Metrics, SystemReporter}
import sdk.hashing.Blake2b
import sdk.history.ByteArray32
import sdk.primitive.ByteArray
import sdk.reflect.ClassesAsConfig
import sdk.syntax.all.*
import sim.Config as SimConfig
import sim.NetworkSnapshot.{reportSnapshot, NodeSnapshot}
import weaver.WeaverState
import weaver.data.*

import java.nio.file.Files
import scala.concurrent.duration.{Duration, DurationInt}

object NetworkSim extends IOApp {

  final private case class Config(
    sim: SimConfig,
    node: NodeConfig,
    influxDb: InfluxDbConfig,
  )

  final case class NetNode[F[_]](
    id: ByteArray,
    node: Node[F],
    balanceApi: (ByteArray32, ByteArray) => F[Option[Balance]],
    getData: F[NodeSnapshot],
  )

  def sim[F[_]: Async: Parallel: Random: Console: KamonContextStore](
    netCfg: SimConfig,
    nodeCfg: node.Config,
    ifxDbCfg: diagnostics.metrics.Config,
  ): Stream[F, Unit] = {
    val rnd                   = new scala.util.Random()
    /// Users (wallets) making transactions
    val users: Set[ByteArray] =
      (1 to netCfg.usersNum).map(_ => Array(rnd.nextInt().toByte)).map(Blake2b.hash256).map(ByteArray(_)).toSet

    val genesisState = new BalancesState(users.map(_ -> Long.MaxValue / 2).toMap)

    /// Genesis data
    val lazinessTolerance = 1 // c.lazinessTolerance
    val senders           =
      Iterator.range(0, netCfg.size).map(_ => Array(rnd.nextInt().toByte)).map(Blake2b.hash256).map(ByteArray(_)).toSet
    // Create lfs message, it has no parents, sees no offences and final fringe is empty set
    val genesisBonds      = Bonds(senders.map(_ -> 100L).toMap)
    val genesisExec       = FinalData(genesisBonds, lazinessTolerance, 10000)
    val lfs               = MessageData[ByteArray, ByteArray](ByteArray("s0".getBytes), Set(), Set(), FringeData(Set()), genesisExec)

    // Shared blockStore for simulation
    val blockStore @ (saveBlock, _) = Setup.mkBlockStore[F, ByteArray, ByteArray, BalancesDeploy]

    // Shared tx store
    val (saveTx, readTx) = Setup.mkDeployPool[F]

    def broadcast(
      peers: List[Node[F]],
      time: Duration,
    ): Pipe[F, ByteArray, Unit] = _.evalMap(m => Temporal[F].sleep(time) *> peers.traverse(_.dProc.acceptMsg(m)).void)

    def random(users: Set[ByteArray]): F[BalancesState] = for {
      txVal <- Random[F].nextLongBounded(100)
      from  <- Random[F].elementOf(users)
      to    <- Random[F].elementOf(users - from)
    } yield new BalancesState(Map(from -> -txVal, to -> txVal))

    def mkNode(vId: ByteArray, idx: Int): Resource[F, NetNode[F]] = {
      val dataPath = Files.createTempDirectory(s"gorki-sim-node-$vId")

      Setup
        .metrics(enableInfluxDb = nodeCfg.enableInfluxDb, ifxDbCfg, vId.toHex)
        .flatMap { case metrics =>
          implicit val x: Metrics[F] = metrics

          val blockSeqNumRef = Ref.unsafe(0)
          val assignBlockId  = (b: Block[ByteArray, ByteArray, BalancesDeploy]) =>
            blockSeqNumRef.updateAndGet(_ + 1).map { seqNum =>
              ByteArray(Blake2b.hash256(b.digest.bytes))
            }

          val txSeqNumRef = Ref.unsafe(0)
          val nextTxs     = txSeqNumRef
            .updateAndGet(_ + 1)
            .flatMap { idx =>
              random(users).map(st => BalancesDeploy(BalancesDeployBody(st, idx.longValue)))
            }
            .replicateA(netCfg.txPerBlock)
            .flatTap(_.traverse(saveTx))
            .map(_.toSet)

          val netNode = Node
            .apply[F](
              vId,
              WeaverState.empty[ByteArray, ByteArray, BalancesDeploy](lfs.state),
              assignBlockId,
              dataPath,
              blockStore,
              nextTxs,
              nodeCfg.copy(webApi = nodeCfg.webApi.copy(port = nodeCfg.webApi.port + idx)),
            )
            .map { case (node, (lfsHashF, readBalanceF)) =>
              val getSnapshot =
                (
                  node.weaverStRef.get,
                  node.propStRef.get,
                  node.procStRef.get,
                  node.bufferStRef.get,
                ).flatMapN { case (w, p, pe, b) =>
                  lfsHashF.map(State(w, p, pe, b, _))
                }

              val tpsRef    = Ref.unsafe[F, Double](0f)
              val tpsUpdate = node.dProc.finStream
                .map(_.accepted.toList)
                .flatMap(Stream.emits(_))
                .throughput(1.second)
                // finality is computed by each sender eventually so / c.size
                .map(_.toDouble / netCfg.size)
                .evalTap(tpsRef.set)
              val getData   = (getSnapshot, tpsRef.get).mapN { case State(w, p, pe, b, lfsHash) -> tps =>
                NodeSnapshot(vId, tps.toFloat, tps.toFloat / netCfg.txPerBlock, State(w, p, pe, b, lfsHash))
              }

              val animateDiag = Stream
                .repeatEval(getData)
                .metered(1.second)
                .evalTap { x =>
                  implicit val m: Metrics[F] = metrics
                  reportSnapshot(x)
                }
                .map(v => s"\u001b[2J${v.show}")
                .printlns

              NetNode[F](
                vId,
                node.copy(dProc =
                  node.dProc
                    .copy(dProcStream =
                      node.dProc.dProcStream concurrently tpsUpdate concurrently
                        SystemReporter[F]() concurrently animateDiag,
                    ),
                ),
                readBalanceF,
                getData,
              )
            }
          netNode
        }
    }

    Stream
      .resource(lfs.state.bonds.activeSet.toList.zipWithIndex.traverse((mkNode _).tupled))
      .map(_.zipWithIndex)
      .map { net =>
        net.map {
          case NetNode(
                self,
                Node(weaverStRef, _, _, _, dProc, exeEngine),
                readBalance,
                getData,
              ) -> idx =>
            val mkGenesis = {
              implicit val m: Metrics[F] = Metrics.unit
              Stream.eval(Genesis.mkGenesisBlock[F](senders.head, genesisExec, genesisState, exeEngine).flatMap {
                genesisM =>
                  val genesis = genesisM.m.txs.head
                  saveBlock(genesisM) *> saveTx(genesis) *> dProc.acceptMsg(genesisM.id) *>
                    Console[F].println(s"Bootstrap done for ${self}")
              })
            }.whenA(idx == 0)
            val notSelf   = net.collect { case NetNode(id, node, _, _) -> _ if id != self => node }

            val run = dProc.dProcStream concurrently {
              dProc.output.through(
                broadcast(notSelf, netCfg.propDelay)
                  // add execution delay
                  .compose(x => Stream.eval(Async[F].sleep(netCfg.exeDelay).replicateA(netCfg.txPerBlock)) *> x),
              )
            }

            (run concurrently mkGenesis) -> getData
        }
      }
      .map(_.unzip)
      .flatMap { case (streams, diags) =>
        val simStream = Stream.emits(streams).parJoin(streams.size)

        val logDiag = {
          val getNetworkState = diags.sequence
          getNetworkState.showAnimated(samplingTime = 150.milli)
        }

        simStream concurrently logDiag
      }
  }

  override def run(args: List[String]): IO[ExitCode] = {
    val prompt = """
    This application simulates the network of nodes with the following features:
      1. Speculative execution (block merge).
      2. Garbage collection of the consensus state.
      3. Synchronous flavour of a consensus protocol.
      4. The state that the network agrees on is a map of wallet balances.
      5. A transaction is a move of some random amount from one wallet to another.

    Blocks are created by all nodes as fast as possible. Number of transactions per block can be adjusted
      with the argument. Transactions are generated randomly.

    Usage:
      Run simulation:           java -Dconfig.file=<path to config file> -jar sim.jar run
      Dump default config file: java -jar sim.jar --print-default-config > <path>

    Output: console animation of the diagnostics data read from nodes. One line per node, sorted by the node index.

      BPS | Consensus size | Proposer status | Processor size | History size | LFS hash
    110.0         23         Creating            0 / 0(10)           3456          a71bbe62ee03c16498b9d975501f4063e8ca344f9f5b1efb95aedc13e432393e
    110.0         23             Idle            1 / 0(10)           3456          a71bbe62ee03c16498b9d975501f4063e8ca344f9f5b1efb95aedc13e432393e
    110.0         23             Idle            1 / 0(10)           3456          a71bbe62ee03c16498b9d975501f4063e8ca344f9f5b1efb95aedc13e432393e
    110.0         23             Idle            1 / 0(10)           3456          a71bbe62ee03c16498b9d975501f4063e8ca344f9f5b1efb95aedc13e432393e

      BPS             - blocks finalized per second (measured on the node with index 0).
      Consensus size  - number of blocks in the consensus state.
      Proposer status - status of the block proposer.
      Processor size  - number of blocks currently in processing / waiting for processing.
      History size    - blockchain state size. Number of records in key value store underlying the radix tree.
                      Keys are integers and values are longs.
      LFS hash        - hash of the oldest onchain state required for the node to operate (hash of last finalized state).

      In addition to console animation each node exposes its API via http on the port 808<i> where i is the index
        of the node.

    Available API endpoints:
      - latest blocks node observes from each peer
      http://127.0.0.1:8080/api/v1/latest

      - status
      http://127.0.0.1:8080/api/v1/status

      - block given id
      http://127.0.0.1:8080/api/v1/block/<block_id>
      Example: http://127.0.0.1:8080/api/v1/block/genesis

      - balance of a wallet given its id for historical state identified by hash
      http://127.0.0.1:8080/api/v1/balance/<hash>/<wallet_id>
      Example: http://127.0.0.1:8080/api/v1/balances/7da2990385661697cf7017a206084625720439429c26a580783ab0768a80251d/1

      - deploy given id
      http://127.0.0.1:8080/api/v1/deploy/<deploy_id>
      Example: http://127.0.0.1:8080/api/v1/deploy/genesis

    """.stripMargin

    import pureconfig.*
    import pureconfig.generic.auto.*

    implicit def hint[A]: ProductHint[A] = ProductHint[A](ConfigFieldMapping(CamelCase, CamelCase))

    args match {
      case List("--help")                 => IO.println(prompt).as(ExitCode.Success)
      case List("--print-default-config") =>
        val referenceConf = ClassesAsConfig(
          "gorki",
          InfluxDbConfig.Default,
          NodeConfig.Default,
          SimConfig.Default,
        )
        IO.println(referenceConf).as(ExitCode.Success)

      case List("run") =>
        final case class GorkiConfig(gorki: Config)
        val loadConfig     = ConfigSource.default
          .load[GorkiConfig]
          .map(_.gorki)
          .leftTraverse[IO, Config] { err =>
            new Exception("Invalid configuration file", new Exception(err.toList.map(_.description).mkString("\n")))
              .raiseError[IO, Config]
          }
          .map(_.merge)
        val mkContextStore = KamonContextStore.forCatsEffectIOLocal
        val mkPrng         = Random.scalaUtilRandom[IO]

        (loadConfig, mkContextStore, mkPrng).flatMapN {
          case (Config(network, node, influxDb), ioLocalKamonContext, prng) =>
            implicit val x: KamonContextStore[IO] = ioLocalKamonContext
            implicit val y: Random[IO]            = prng

            if (node.persistOnChainState) {
              NetworkSim.sim[IO](network, node, influxDb).compile.drain.as(ExitCode.Success)
            } else {
              // in memory cannot run forever so restart each minute
              Stream
                .eval(SignallingRef.of[IO, Boolean](false))
                .flatMap { sRef =>
                  val resetStream = Stream.sleep[IO](1.minutes) ++ Stream.eval(sRef.set(true))
                  NetworkSim.sim[IO](network, node, influxDb).interruptWhen(sRef) concurrently resetStream
                }
                .repeat
                .compile
                .drain
                .as(ExitCode.Success)
            }
        }
      case x           => IO.println(s"Illegal option '${x.mkString(" ")}': see --help").as(ExitCode.Error)
    }
  }
}
