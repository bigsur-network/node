package sdk.consensus

import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import sdk.consensus.data.BondsMap
import sdk.syntax.all.sdkSyntaxTry

class BondsMapSpec extends AsyncFlatSpec with Matchers {
  "BondsMap application" should "fail if bonds map is empty" in {
    BondsMap(Map.empty[Int, Long]) shouldBe a[scala.util.Failure[*]]
  }

  "BondsMap application" should "fail if total stake is larger then Long max value" in {
    BondsMap(Map(1 -> Long.MaxValue, 2 -> 1)) shouldBe a[scala.util.Failure[*]]
  }

  "BondsMap application" should "fail if bonds map contains non positive stake" in {
    BondsMap(Map(1 -> 1, 2 -> 0)) shouldBe a[scala.util.Failure[*]]
  }

  "BondsMap application" should "succeed otherwise" in {
    BondsMap(Map(1 -> 1, 2 -> 1)) shouldBe a[scala.util.Success[*]]
  }

  "isSupermajority" should "return false if target stake is less then 2/3 of total stake" in {
    val bondsMap = BondsMap(Map(1 -> 1, 2 -> 1, 3 -> 2)).getUnsafe
    BondsMap.isSuperMajority(bondsMap, Set(1, 2)) shouldBe false
  }

  "isSupermajority" should "return false if target stake is equal to 2/3 of total stake" in {
    val bondsMap = BondsMap(Map(1 -> 1, 2 -> 1, 3 -> 1)).getUnsafe
    BondsMap.isSuperMajority(bondsMap, Set(1, 2)) shouldBe false
  }

  "isSupermajority" should "return trie if target stake is more then 2/3 of total stake" in {
    val bondsMap = BondsMap(Map(1 -> 2, 2 -> 1, 3 -> 1)).getUnsafe
    BondsMap.isSuperMajority(bondsMap, Set(1, 2)) shouldBe true
  }
}
