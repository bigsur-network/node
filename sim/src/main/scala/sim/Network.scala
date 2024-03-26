package sim

import cats.Parallel
import cats.effect.kernel.Async
import cats.syntax.all.*
import fs2.Stream
import node.{Node, NodeSnapshot, Setup}
import sdk.data.BalancesState
import sdk.diag.Metrics
import sdk.primitive.ByteArray
import weaver.data.FinalData

import scala.concurrent.duration.DurationInt

object Network {

  def apply[F[_]: Async: Parallel](
    peers: List[Setup[F]],
    genesisPosState: FinalData[ByteArray],
    genesisBalancesState: BalancesState,
    netCfg: sim.Config,
  ): fs2.Stream[F, Unit] = {
    val streams = peers.map(_.nodeStream)

    val logDiag: Stream[F, Unit] = {
      val streams = peers.map { p =>
        NodeSnapshot
          .stream(p.stateManager, p.dProc.finStream)
          .map(List(_))
          .metered(100.milli)
      }

      streams.tail
        .foldLeft(streams.head) { case (acc, r) => acc.parZipWith(r) { case (acc, x) => x ++ acc } }
        .map(_.zipWithIndex)
        .map(NetworkSnapshot(_))
        .map(_.show)
        .evalMap(x => println(x).pure)
    }

    val simStream: Stream[F, Unit] = Stream.emits(streams).parJoin(streams.size)

    val mkGenesisOnNode0: Stream[F, Unit] = {
      val genesisCreator: Setup[F] = peers.head
      implicit val m: Metrics[F]   = Metrics.unit
      Node.createGenesis(
        genesisPosState.bonds.bonds.head._1,
        genesisPosState,
        genesisBalancesState,
        genesisCreator,
      )
    }

    simStream concurrently logDiag concurrently mkGenesisOnNode0
  }
}
