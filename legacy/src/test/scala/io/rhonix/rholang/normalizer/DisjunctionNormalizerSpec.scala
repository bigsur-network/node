package io.rhonix.rholang.normalizer

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import coop.rchain.rholang.interpreter.compiler.VarSort
import coop.rchain.rholang.interpreter.errors.TopLevelLogicalConnectivesNotAllowedError
import io.rhonix.rholang.normalizer.util.Mock.*
import io.rhonix.rholang.normalizer.util.MockNormalizerRec.mockADT
import io.rhonix.rholang.ast.rholang.Absyn.*
import io.rhonix.rholang.types.ConnOrN
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class DisjunctionNormalizerSpec extends AnyFlatSpec with ScalaCheckPropertyChecks with Matchers {

  behavior of "Disjunction normalizer"

  it should "normalize PDisjunction term" in {
    forAll { (s1: String, s2: String) =>
      val left  = new PGround(new GroundString(s1))
      val right = new PGround(new GroundString(s2))
      val term  = new PDisjunction(left, right)

      implicit val (nRec, _, _, _, _, _, _, _, rReader) = createMockDSL[IO, VarSort](patternDepth = 1)

      val adt = DisjunctionNormalizer.normalizeDisjunction[IO](term).unsafeRunSync()

      val expectedAdt = ConnOrN(Seq(mockADT(left: Proc), mockADT(right: Proc)))

      adt shouldBe expectedAdt

      val terms         = nRec.extractData
      // Expect both sides of conjunction to be normalized in sequence
      val expectedTerms = Seq(TermData(ProcTerm(left)), TermData(ProcTerm(right)))

      terms shouldBe expectedTerms
    }
  }

  it should "throw an exception when attempting to normalize the top-level term" in {
    val term = new PDisjunction(new PNil, new PNil)

    // Create a mock DSL with the true `isTopLevel` flag (default value).
    implicit val (nRec, _, _, _, _, _, _, _, rReader) = createMockDSL[IO, VarSort]()

    val thrown = intercept[TopLevelLogicalConnectivesNotAllowedError] {
      DisjunctionNormalizer.normalizeDisjunction[IO](term).unsafeRunSync()
    }

    thrown.getMessage should include("disjunction")
  }
}
