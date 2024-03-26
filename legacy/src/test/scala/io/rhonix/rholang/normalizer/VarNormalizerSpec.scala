package io.rhonix.rholang.normalizer

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import coop.rchain.rholang.interpreter.compiler.{NameSort, ProcSort, VarSort}
import coop.rchain.rholang.interpreter.errors.*
import io.rhonix.rholang.normalizer.util.Mock.*
import io.rhonix.rholang.ast.rholang.Absyn.*
import io.rhonix.rholang.types.{BoundVarN, FreeVarN, WildcardN}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class VarNormalizerSpec extends AnyFlatSpec with ScalaCheckPropertyChecks with Matchers {

  behavior of "Var normalizer"

  it should "return the Rholang bound variable if the BoundVarMap contains a variable with the same name" in {
    forAll { (varName: String, varIndex: Int) =>
      val term = new PVar(new ProcVarVar(varName))

      implicit val (_, _, _, bVR, _, fVW, fVR, _, infoReader) = createMockDSL[IO, VarSort](
        // Add a variable with the name `varName` and the index `varIndex` to the boundVarMap
        initBoundVars = Map(varName -> (varIndex, ProcSort)),
      )

      val par = VarNormalizer.normalizeVar[IO, VarSort](term).unsafeRunSync()
      par shouldBe BoundVarN(-1)
    }
  }

  it should "throw an exception if the type of the bound variable in the map is not ProcSort" in {
    forAll { (varName: String, varIndex: Int) =>
      val term = new PVar(new ProcVarVar(varName))

      implicit val (_, _, _, bVR, _, fVW, fVR, _, infoReader) = createMockDSL[IO, VarSort](
        // Add a variable with an unexpected type (NameSort)
        initBoundVars = Map(varName -> (varIndex, NameSort)),
      )

      val thrown = intercept[UnexpectedProcContext] {
        VarNormalizer.normalizeVar[IO, VarSort](term).unsafeRunSync()
      }

      thrown.getMessage should include(varName)
    }
  }

  it should "return the Rholang free variable if the BoundVarMap doesn't contain a variable with the same name" in {
    forAll { (varName: String) =>
      val term = new PVar(new ProcVarVar(varName))

      // Create a mock DSL with an empty BoundVarMap and FreeVarMap, and with the false `isTopLevel` flag.
      implicit val (_, _, _, bVR, _, fVW, fVR, _, infoReader) = createMockDSL[IO, VarSort](patternDepth = 1)

      val par = VarNormalizer.normalizeVar[IO, VarSort](term).unsafeRunSync()

      val addedFreeVars    = fVW.extractData
      val expectedFreeVars = Seq(FreeVarWriterData(name = varName, varType = ProcSort))
      // A free variable should be added to the freeVarMap.
      addedFreeVars shouldBe expectedFreeVars

      // Should be returned Rholang free variable.
      par shouldBe FreeVarN(DefFreeVarIndex)
    }
  }

  it should "throw an exception when trying to add a free variable to the top-level term (not in the pattern)" in {
    forAll { (varName: String) =>
      val term = new PVar(new ProcVarVar(varName))

      // Create a mock DSL with the true `isTopLevel` flag (default value).
      implicit val (_, _, _, bVR, _, fVW, fVR, _, infoReader) = createMockDSL[IO, VarSort]()

      val thrown = intercept[TopLevelFreeVariablesNotAllowedError] {
        VarNormalizer.normalizeVar[IO, VarSort](term).unsafeRunSync()
      }

      thrown.getMessage should include(varName)
    }
  }

  it should "throw an exception if a free variable is used twice as a binder" in {
    forAll { (varName: String, varIndex: Int) =>
      val term = new PVar(new ProcVarVar(varName))

      implicit val (_, _, _, bVR, _, fVW, fVR, _, infoReader) = createMockDSL[IO, VarSort](
        // Add a free variable with the same name
        initFreeVars = Map(varName -> (varIndex, NameSort)),
        patternDepth = 1,
      )

      val thrown = intercept[UnexpectedReuseOfProcContextFree] {
        VarNormalizer.normalizeVar[IO, VarSort](term).unsafeRunSync()
      }

      thrown.getMessage should include(varName)
    }
  }

  it should "return the Rholang wildcard for wildcard term inside a pattern" in {
    val term = new PVar(new ProcVarWildcard)

    implicit val (_, _, _, bVR, _, fVW, fVR, _, infoReader) = createMockDSL[IO, VarSort](
      patternDepth = 1,
    )

    val par = VarNormalizer.normalizeVar[IO, VarSort](term).unsafeRunSync()
    par shouldBe WildcardN
  }

  it should "throw an exception when trying to add a wildcard to the top-level term (not in the pattern)" in {
    val term = new PVar(new ProcVarWildcard)

    // Create a mock DSL with the true `isTopLevel` flag (default value).
    implicit val (_, _, _, bVR, _, fVW, fVR, _, infoReader) = createMockDSL[IO, VarSort]()

    val thrown = intercept[TopLevelWildcardsNotAllowedError] {
      VarNormalizer.normalizeVar[IO, VarSort](term).unsafeRunSync()
    }

    thrown.getMessage should include("wildcard")
  }
}
