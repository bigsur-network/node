package node.balances

import cats.syntax.all.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sdk.primitive.ByteArray
import sdk.merging.MergeLogicForPayments.*
import sdk.data.{BalancesDeploy, BalancesDeployBody, BalancesState}

class MergeLogicForPaymentsSpec extends AnyFlatSpec with Matchers {

  behavior of "attemptCombine"

  it should "compute valid output" in {
    val initBalances = Map(ByteArray(Array(1.byteValue)) -> 4L, ByteArray(Array(2.byteValue)) -> 1L)
    val change       = Map(ByteArray(Array(1.byteValue)) -> -1L)
    val reference    = Map(ByteArray(Array(1.byteValue)) -> 3L, ByteArray(Array(2.byteValue)) -> 1L)

    val b   = new BalancesState(initBalances)
    val neg = BalancesDeploy(ByteArray(List()), BalancesDeployBody(new BalancesState(change), 0))
    attemptCombine(b, neg).map(_.diffs) shouldBe new BalancesState(reference).diffs.some
  }

  it should "handle edge case" in {
    val initBalances = Map(ByteArray(Array(1.byteValue)) -> 1L)
    val zero         = Map(ByteArray(Array(1.byteValue)) -> -1L)

    val b        = new BalancesState(initBalances)
    val zeroCase = BalancesDeploy(ByteArray(List()), BalancesDeployBody(new BalancesState(zero), 0))
    attemptCombine(b, zeroCase).map(_.diffs).isDefined shouldBe true
  }

  it should "reject deploy if leads negative" in {
    val initBalances = Map(ByteArray(Array(1.byteValue)) -> 1L)
    val changeNeg    = Map(ByteArray(Array(1.byteValue)) -> -2L)

    val b   = new BalancesState(initBalances)
    val neg = BalancesDeploy(ByteArray(List()), new BalancesDeployBody(BalancesState(changeNeg), 0))
    attemptCombine(b, neg) shouldBe None
  }

  it should "throw exception on Long overflow" in {
    val initBalances = Map(ByteArray(Array(1.byteValue)) -> Long.MaxValue)
    val changeNeg    = Map(ByteArray(Array(1.byteValue)) -> 1L)

    val b   = new BalancesState(initBalances)
    val neg = BalancesDeploy(ByteArray(List()), new BalancesDeployBody(BalancesState(changeNeg), 0))
    intercept[Exception](attemptCombine(b, neg))
  }

  behavior of "foldCollectFailures"

  it should "output correct combination result and failures" in {
    val state = 4
    val items = Seq(1, 2, 3)

    def attemptCombine(state: Int, item: Int): Option[Int] = item match {
      case 3 => none[Int]
      case x => (state + x).some
    }

    foldCollectFailures(state, items, attemptCombine) shouldBe (7, Seq(3))
  }
}
