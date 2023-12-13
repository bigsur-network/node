package sdk.consensus

import org.scalacheck.{Arbitrary, Gen}
import sdk.consensus.data.BondsMap

trait ArbInstances {
  val arbSmallPositiveLong: Arbitrary[Long] = Arbitrary(Gen.posNum[Long])

  implicit def arbBondsMap[A: Arbitrary]: Arbitrary[BondsMap[A]] = {
    // Override the default Long generator to generate small Longs and make bonds map overflow less likely.
    implicit val a: Arbitrary[Long] = arbSmallPositiveLong
    Arbitrary(Arbitrary.arbitrary[Map[A, Long]].map(BondsMap(_)).filter(_.isSuccess).map(_.get))
  }
}
