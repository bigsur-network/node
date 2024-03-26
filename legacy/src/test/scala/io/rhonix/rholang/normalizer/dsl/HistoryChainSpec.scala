package io.rhonix.rholang.normalizer.dsl

import io.rhonix.rholang.normalizer.envimpl.HistoryChain
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class HistoryChainSpec extends AnyFlatSpec with Matchers {

  "A HistoryChain" should "return the current element when calling current" in {
    val historyChain = HistoryChain(Seq(1, 2, 3))
    historyChain.current() shouldBe 3
  }

  it should "return the correct depth when calling depth" in {
    val historyChain = HistoryChain(Seq(1, 2, 3))
    historyChain.depth shouldBe 3
  }

  it should "return an iterator when calling iter" in {
    val historyChain = HistoryChain(Seq(1, 2, 3))
    historyChain.iter.toList shouldBe (List(1, 2, 3))
  }

  it should "append a new element when calling push" in {
    val historyChain = HistoryChain(Seq(1, 2, 3))
    historyChain.push(4)
    historyChain.current() shouldBe 4
  }

  it should "append a copy of the last element when calling pushCopy" in {
    val historyChain = HistoryChain(Seq(1, 2, 3))
    historyChain.pushCopy()
    historyChain.current() shouldBe 3
    historyChain.depth shouldBe 4
  }

  it should "remove and return the last element when calling pop" in {
    val historyChain  = HistoryChain(Seq(1, 2, 3))
    val poppedElement = historyChain.pop()
    poppedElement shouldBe 3
    historyChain.current() shouldBe 2
  }

  "An empty HistoryChain" should "be created when calling empty" in {
    val historyChain = HistoryChain.default[Int]
    historyChain.depth shouldBe 0
  }
}
