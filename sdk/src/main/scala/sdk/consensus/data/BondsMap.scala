package sdk.consensus.data

import scala.util.Try

final class BondsMap[A] private (val bonds: Map[A, Long]) extends AnyVal

object BondsMap {
  def apply[A](bonds: Map[A, Long]): Try[BondsMap[A]] = Try {
    require(bonds.nonEmpty, "Bonds map cannot be empty!")
    require(
      Try(bonds.values.foldLeft(0L) { case (a, b) =>
        require(b > 0, "Non positive stake!")
        Math.addExact(a, b)
      }).isSuccess,
      "Total stake exceeds Long!",
    )
    new BondsMap(bonds)
  }

  def unapply[A](bondsMap: BondsMap[A]): Option[Map[A, Long]] = Some(bondsMap.bonds)

  def totalStake[A](bondsMap: BondsMap[A]): Long = bondsMap.bonds.values.sum

  def isSuperMajority[A](bondsMap: BondsMap[A], target: Set[A]): Boolean = {
    val totalStake  = BondsMap.totalStake(bondsMap)
    val targetStake = bondsMap.bonds.view.filterKeys(target).values.sum
    targetStake.toFloat / totalStake > 2f / 3
  }

  def activeSet[A](bondsMap: BondsMap[A]): Set[A] = bondsMap.bonds.keySet

  def allAcross[A](bondsMap: BondsMap[A], target: Set[A]): Boolean = BondsMap.activeSet(bondsMap) == target

}
