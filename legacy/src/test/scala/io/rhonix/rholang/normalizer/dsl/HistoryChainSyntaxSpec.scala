package io.rhonix.rholang.normalizer.dsl

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import io.rhonix.rholang.normalizer.envimpl.HistoryChain
import io.rhonix.rholang.normalizer.syntax.all.normalizerSyntaxHistoryChain
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class HistoryChainSyntaxSpec extends AnyFlatSpec with Matchers {

  "HistoryChainOps" should "update the current element when calling updateCurrent" in {
    val historyChain = HistoryChain(Seq(1, 2, 3))
    historyChain.updateCurrent(_ + 1)
    historyChain.iter.toList shouldBe List(1, 2, 4)
  }

  it should "run a function with new data in the HistoryChain when calling runWithNewDataInChain" in {
    val historyChain = HistoryChain(Seq(1, 2, 3))
    val scopeFn      = IO {
      historyChain.iter.toList shouldBe List(1, 2, 3, 4)
    }
    historyChain.runWithNewDataInChain(scopeFn, 4).unsafeRunSync()
    historyChain.iter.toList shouldBe List(1, 2, 3) // The new data shouldBe popped off after running the function
  }
}
