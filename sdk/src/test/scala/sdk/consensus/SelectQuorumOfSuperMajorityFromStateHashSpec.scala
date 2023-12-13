package sdk.consensus

import org.scalacheck.ScalacheckShapeless.derivedArbitrary
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import sdk.consensus.data.BondsMap
import sdk.primitive.ByteArray

class SelectQuorumOfSuperMajorityFromStateHashSpec extends AsyncFlatSpec with Matchers with ScalaCheckPropertyChecks {
  import sdk.ArbInstances.*

  "next" should "be deterministic and produce quorums of super majority" in {
    forAll { (bonds: BondsMap[Int], stateHash: ByteArray) =>
      val a = SelectQuorumOfSuperMajorityFromStateHash(bonds).next(stateHash)
      val b = SelectQuorumOfSuperMajorityFromStateHash(bonds).next(stateHash)
      a shouldEqual b
      BondsMap.isSuperMajority(bonds, a) shouldBe true
    }
  }
}
