package sdk.consensus

import sdk.consensus.data.BondsMap
import sdk.primitive.ByteArray

object SelectQuorumOfSuperMajorityFromStateHash {
  private def collectSuperMajority[A](bondsMap: Seq[(A, Long)]): Seq[(A, Long)] = {
    val totalStake         = bondsMap.map(_._2).sum
    val superMajorityStake = totalStake * 2 / 3
    bondsMap
      .foldLeft(List.empty[(A, Long)], 0L) { case ((acc, stake), (sender, amount)) =>
        if (stake > superMajorityStake) (acc, stake)
        else ((sender -> amount) +: acc, stake + amount)
      }
      ._1
  }

  /**
   * Quorum selection that deterministically derives subset of bondsMap from the byte array representing stateHash.
   * Output set has to be super majority of the bonds map.
   *
   * @param bondsMap bonds map to select quorum from
   * @tparam A type of a sender
   * @return
   */
  def apply[A: Ordering](bondsMap: BondsMap[A]): SelectQuorum[ByteArray, A] =
    new SelectQuorum[ByteArray, A] {
      override def next(x: ByteArray): Set[A] = {
        // Sort bonds map from current state
        val sorted  = bondsMap.bonds.toVector.sorted
        // Options are rotated bonds map
        val options = LazyList.iterate(sorted) {
          case head +: tail => tail :+ head
          case head         => head
        }
        // Rotate bonds map by the first byte of the state hash and pick the first option.
        val winner  =
          if (sorted.nonEmpty) options.drop(x.bytes.headOption.getOrElse(0.toByte) % sorted.size).head
          else options.head
        collectSuperMajority(winner).map(_._1).toSet
      }
    }
}
