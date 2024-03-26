package io.rhonix.rholang.normalizer

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import coop.rchain.rholang.interpreter.compiler.VarSort
import coop.rchain.rholang.interpreter.errors.TopLevelLogicalConnectivesNotAllowedError
import io.rhonix.rholang.normalizer.util.Mock.*
import io.rhonix.rholang.normalizer.util.MockNormalizerRec.mockADT
import io.rhonix.rholang.ast.rholang.Absyn.*
import io.rhonix.rholang.types.ConnNotN
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class NegationNormalizerSpec extends AnyFlatSpec with ScalaCheckPropertyChecks with Matchers {

  behavior of "Negation normalizer"

  it should "normalize PNegation term" in {
    forAll { (s: String) =>
      val data = new PGround(new GroundString(s))
      val term = new PNegation(data)

      // Create a mock DSL with the false `isTopLevel` flag.
      implicit val (nRec, _, _, _, _, _, _, _, infoReader) = createMockDSL[IO, VarSort](patternDepth = 1)

      val adt = NegationNormalizer.normalizeNegation[IO](term).unsafeRunSync()

      val expectedAdt = ConnNotN(mockADT(data: Proc))

      adt shouldBe expectedAdt

      val terms         = nRec.extractData
      val expectedTerms = Seq(TermData(ProcTerm(data)))

      terms shouldBe expectedTerms

    }
  }

  it should "throw an exception when attempting to normalize the top-level term" in {
    val term = new PNegation(new PNil)

    // Create a mock DSL with the true `isTopLevel` flag (default value).
    implicit val (nRec, _, _, _, _, _, _, _, infoReader) = createMockDSL[IO, VarSort]()

    val thrown = intercept[TopLevelLogicalConnectivesNotAllowedError] {
      NegationNormalizer.normalizeNegation[IO](term).unsafeRunSync()
    }

    thrown.getMessage should include("negation")
  }
}
