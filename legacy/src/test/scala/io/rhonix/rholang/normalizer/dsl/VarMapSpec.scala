package io.rhonix.rholang.normalizer.dsl

import coop.rchain.rholang.interpreter.compiler.SourcePosition
import io.rhonix.rholang.normalizer.env.VarContext
import io.rhonix.rholang.normalizer.envimpl.VarMap
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class VarMapSpec extends AnyFlatSpec with Matchers {

  "A VarMap" should "return None when getting a non-existing variable" in {
    val varMap = VarMap.default[Int]
    varMap.get("nonExistingVar") shouldBe None
  }

  it should "return the correct VarContext when getting an existing variable" in {
    val varMap = VarMap(Seq(("existingVar", 1, SourcePosition(0, 0))))
    varMap.get("existingVar") shouldBe (Some(VarContext(0, 0, 1, SourcePosition(0, 0))))
  }

  it should "return all variables when calling getAll" in {
    val varMap = VarMap(
      Seq(
        ("var1", 1, SourcePosition(0, 0)),
        ("var2", 2, SourcePosition(1, 1)),
      ),
    )
    varMap.data.toSeq should contain theSameElementsAs Seq(
      ("var1", VarContext(0, -1, 1, SourcePosition(0, 0))),
      ("var2", VarContext(1, -1, 2, SourcePosition(1, 1))),
    )
  }

  it should "update the VarContext when putting an existing variable" in {
    val varMap = VarMap(Seq(("existingVar", 1, SourcePosition(0, 0))))
      .put("existingVar", 2, SourcePosition(1, 1))
    varMap._1.get("existingVar") shouldBe (Some(VarContext(1, 0, 2, SourcePosition(1, 1))))
  }

  it should "increment the de Bruijn index when putting a new variable" in {
    val varMap = VarMap(Seq(("var1", 1, SourcePosition(0, 0))))
      .put("var2", 2, SourcePosition(1, 1))
    varMap._1.get("var1").get.index shouldBe 0
    varMap._1.get("var2").get.index shouldBe 1
  }
}
