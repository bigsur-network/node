package sim

import cats.Show
import cats.syntax.all.*
import node.NodeSnapshot

final case class NetworkSnapshot[M, S, T](nodes: List[(NodeSnapshot[M, S, T], Int)])

/// Snapshot of the simulation state.
object NetworkSnapshot {

  implicit def showNetworkSnapshot[M, S: Ordering, T]: Show[NetworkSnapshot[M, S, T]] =
    new Show[NetworkSnapshot[M, S, T]] {
      override def show(x: NetworkSnapshot[M, S, T]): String =
        s"""Id | TPS | Consensus size | Proposer status | Processor size | LFS hash
           |${x.nodes.sortBy(_._2).map { case (nS, idx) => s"$idx ${nS.show}" }.mkString("\n")}
           |""".stripMargin
    }

}
