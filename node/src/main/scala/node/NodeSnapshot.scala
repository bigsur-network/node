package node

import cats.effect.{Async, Ref}
import cats.syntax.all.*
import cats.{Applicative, Show}
import fs2.Stream
import node.Hashing.*
import node.state.{State, StateManager}
import sdk.DagCausalQueue
import sdk.data.BalancesDeploy
import sdk.diag.Metrics
import sdk.diag.Metrics.Field
import sdk.history.{ByteArray32, History}
import sdk.node.{Processor, Proposer}
import sdk.primitive.ByteArray
import sdk.syntax.all.{fs2StreamSyntax, sdkSyntaxByteArray}
import weaver.WeaverState
import weaver.data.ConflictResolution

import scala.concurrent.duration.DurationInt

final case class NodeSnapshot[M, S, T](
  // transactions finalized per second at the moment of snapshot creation
  tps: Float,
  // states
  weaver: WeaverState[M, S, T],
  proposer: Proposer.ST,
  processor: Processor.ST[M],
  buffer: DagCausalQueue[M],
  lfsHash: ByteArray32,
)

object NodeSnapshot {
  def stream[F[_]: Async](
    state: StateManager[F],
    finalizedStream: Stream[F, ConflictResolution[BalancesDeploy]],
  ): Stream[F, NodeSnapshot[ByteArray, ByteArray, BalancesDeploy]] = {
    val tpsRef = Ref.unsafe[F, Double](0f)

    val lfsHashF = (state.fringeMappingRef.get, state.weaverStRef.get).mapN { case (fM, w) =>
      w.lazo.fringes
        .minByOption { case (i, _) => i }
        .map { case (_, fringe) => fringe }
        .flatMap(fM.get)
        .getOrElse(History.EmptyRootHash)
    }

    val getSnapshot =
      (
        state.weaverStRef.get,
        state.propStRef.get,
        state.procStRef.get,
        state.bufferStRef.get,
      ).flatMapN { case (w, p, pe, b) =>
        lfsHashF.map(State(w, p, pe, b, _))
      }

    val tpsUpdate = finalizedStream
      .throughput(1.second)
      // finality is computed by each sender eventually so / c.size
      .map(_.toDouble)
      .evalTap(tpsRef.set)

    val getData = (getSnapshot, tpsRef.get).mapN { case State(w, p, pe, b, lfsHash) -> tps =>
      NodeSnapshot[ByteArray, ByteArray, BalancesDeploy](tps.toFloat, w, p, pe, b, lfsHash)
    }

    Stream.repeatEval(getData) concurrently tpsUpdate
  }

  def reportSnapshot[F[_]: Metrics: Applicative, M, S, T](s: NodeSnapshot[M, S, T]): F[Unit] =
    Metrics[F].measurement(
      "nodeSnapshot",
      List(
        Field("tps", s.tps.toDouble.asInstanceOf[AnyRef]),
        Field("consensusSize", s.weaver.lazo.dagData.size.asInstanceOf[AnyRef]),
        Field("processorSize", s.processor.processingSet.size.asInstanceOf[AnyRef]),
        Field("latestMessages", s.weaver.lazo.latestMessages.mkString(" | ")),
        Field(
          "blockHeight",
          s.weaver.lazo.latestMessages.map(s.weaver.lazo.dagData(_).seqNum).maxOption.getOrElse(0).toString,
        ),
      ),
    )

  implicit def showNodeSnapshot[M, S, T]: Show[NodeSnapshot[M, S, T]] = new Show[NodeSnapshot[M, S, T]] {
    override def show(x: NodeSnapshot[M, S, T]): String = {
      import x.*
      val processorData = s"${processor.processingSet.size} / " +
        s"${processor.waitingList.size}(${processor.concurrency})"

      f"$tps%5s ${weaver.lazo.dagData.size}%10s " +
        f"${proposer.status}%16s " +
        f"$processorData%20s " +
        f"${lfsHash.bytes.toHex}%74s"
    }
  }

}
