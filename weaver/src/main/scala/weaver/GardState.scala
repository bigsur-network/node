package weaver

import weaver.GardState.*

import scala.collection.immutable.SortedMap

/** TODO cleanup state */
final case class GardState[M, T](
  fringeIndices: Map[Set[M], Int],
  txsByFringe: SortedMap[Int, Set[T]],
) {

  /** Modify the state by adding data about new message. */
  def add(d: GardM[M, T]): GardState[M, T] = {
    val fringeIdx =
      fringeIndices.getOrElse(d.fringe, txsByFringe.lastOption.map(_._1).getOrElse(Int.MinValue))
    val newFi     = fringeIndices + (d.fringe -> fringeIdx)
    val newMbf    =
      txsByFringe.updated(fringeIdx, txsByFringe.get(fringeIdx).map(_ ++ d.txs).getOrElse(d.txs))
    GardState(newFi, newMbf)
  }

  /** Check whether transaction is a double spend (transaction replay). */
  def isDoubleSpend(tx: T, fringe: Set[M], expT: Int): Boolean = {
    val curFringeIdx = fringeIndices.getOrElse(fringe, 0)
    // For each fringe index between current and current - expT check whether tx appears in a
    // set of transactions proposed on top of this fringe.
    Iterator
      .range(Math.max(curFringeIdx - expT, 0), curFringeIdx)
      .map(txsByFringe.getOrElse(_, Set()))
      .exists(_.contains(tx))
  }
}

object GardState {

  def empty[M, T]: GardState[M, T] = GardState(Map.empty[Set[M], Int], SortedMap.empty[Int, Set[T]])

  /**
   * Data required for the protocol.
   *
   * @param tx     transaction ID
   * @param fringe fringe computed by the message that transaction is part of
   */
  final case class GardM[M, T](txs: Set[T], fringe: Set[M])
}
