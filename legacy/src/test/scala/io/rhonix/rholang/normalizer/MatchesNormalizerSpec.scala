package io.rhonix.rholang.normalizer

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import coop.rchain.rholang.interpreter.compiler.VarSort
import io.rhonix.rholang.normalizer.util.Mock.*
import io.rhonix.rholang.normalizer.util.MockNormalizerRec.mockADT
import io.rhonix.rholang.ast.rholang.Absyn.*
import io.rhonix.rholang.types.EMatchesN
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class MatchesNormalizerSpec extends AnyFlatSpec with ScalaCheckPropertyChecks with Matchers {

  behavior of "Matches normalizer"

  it should "normalize PMatches term" in {
    forAll { (targetStr: String, patternStr: String) =>
      val targetTerm  = new PVar(new ProcVarVar(targetStr))
      val patternTerm = new PGround(new GroundString(patternStr))
      val matchesTerm = new PMatches(targetTerm, patternTerm)

      implicit val (nRec, bVScope, _, _, fVScope, _, _, infoWriter, _) = createMockDSL[IO, VarSort]()

      val adt = MatchesNormalizer.normalizeMatches[IO](matchesTerm).unsafeRunSync()

      val expectedAdt = EMatchesN(
        target = mockADT(targetTerm: Proc),
        pattern = mockADT(patternTerm: Proc),
      )

      adt shouldBe expectedAdt

      val terms = nRec.extractData

      val expectedTerms = Seq(
        TermData(ProcTerm(targetTerm)),
        TermData(term = ProcTerm(patternTerm), boundNewScopeLevel = 1, freeScopeLevel = 1, insidePattern = true),
      )

      terms shouldBe expectedTerms
    }
  }
}
