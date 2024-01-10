package sim

import cats.syntax.all.*
import cats.{Applicative, Show}
import node.Node
import sdk.diag.Metrics
import sdk.diag.Metrics.Field
import sdk.primitive.ByteArray
import sdk.syntax.all.sdkSyntaxByteArray

/// Snapshot of the simulation state.
object NetworkSnapshot {
  final case class NodeSnapshot(
    // id of the node
    id: ByteArray,
    // transactions finalized per second at the moment of snapshot creation
    tps: Float,
    // blocks finalized per second at the moment of snapshot creation
    bps: Float,
    // states
    state: Node.State,
  )

  def reportSnapshot[F[_]: Metrics: Applicative, M, S, T](s: NodeSnapshot): F[Unit] =
    Metrics[F].measurement(
      "nodeSnapshot",
      List(
        Field("tps", s.tps.toDouble.asInstanceOf[AnyRef]),
        Field("bps", s.bps.toDouble.asInstanceOf[AnyRef]),
        Field("consensusSize", s.state.weaver.lazo.dagData.size.asInstanceOf[AnyRef]),
        Field("processorSize", s.state.processor.processingSet.size.asInstanceOf[AnyRef]),
        Field("latestMessages", s.state.weaver.lazo.latestMessages.mkString(" | ")),
        Field(
          "blockHeight",
          s.state.weaver.lazo.latestMessages.map(s.state.weaver.lazo.dagData(_).seqNum).maxOption.getOrElse(0).toString,
        ),
      ),
    )

  implicit def showNodeSnapshot: Show[NodeSnapshot] = new Show[NodeSnapshot] {
    override def show(x: NodeSnapshot): String = {
      import x.*
      val processorData = s"${state.processor.processingSet.size} / " +
        s"${state.processor.waitingList.size}(${state.processor.concurrency})"

      f"$tps%5s $bps%5s ${state.weaver.lazo.dagData.size}%10s " +
        f"${state.proposer.status}%16s " +
        f"$processorData%20s " +
        f"${state.lfsHash.bytes.toHex}%74s"
    }
  }

  implicit def showNetworkSnapshot[M, S: Ordering, T]: Show[List[NodeSnapshot]] =
    new Show[List[NodeSnapshot]] {
      override def show(x: List[NodeSnapshot]): String =
        s"""  TPS | BPS | Consensus size | Proposer status | Processor size | LFS hash
           |${x.sortBy(_.id).map(_.show).mkString("\n")}
           |""".stripMargin
    }

}
