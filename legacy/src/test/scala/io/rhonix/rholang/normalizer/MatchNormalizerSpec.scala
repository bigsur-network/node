package io.rhonix.rholang.normalizer

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import coop.rchain.rholang.interpreter.compiler.{ProcSort, VarSort}
import io.rhonix.rholang.normalizer.util.Mock.*
import io.rhonix.rholang.normalizer.util.MockNormalizerRec.mockADT
import io.rhonix.rholang.ast.rholang.Absyn.*
import io.rhonix.rholang.types.{MatchCaseN, MatchN}
import org.scalacheck.Arbitrary
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalacheck.Arbitrary.arbString

class MatchNormalizerSpec extends AnyFlatSpec with ScalaCheckPropertyChecks with Matchers {

  behavior of "Match normalizer"

  it should "normalize PMatch term" in {
    forAll(Arbitrary.arbitrary[String], Arbitrary.arbitrary[Seq[(String, String)]]) {
      (targetStr: String, casesDataStr: Seq[(String, String)]) =>
        val targetTerm = new PVar(new ProcVarVar(targetStr))

        val casesDataTerm = casesDataStr.map { case (patternStr, caseBodyStr) =>
          val patternTerm  = new PVar(new ProcVarVar(patternStr))
          val caseBodyTerm = new PVar(new ProcVarVar(caseBodyStr))
          (patternTerm, caseBodyTerm)
        }
        val cases         = casesDataTerm.map { case (patternTerm, caseBodyTerm) =>
          new CaseImpl(patternTerm, caseBodyTerm)
        }

        val listCases = new ListCase()
        cases.foreach(listCases.add)

        // match target { case pattern1 => caseBody1; case pattern2 => caseBody2; ... }
        val inputTerm = new PMatch(targetTerm, listCases)

        implicit val (nRec, bVScope, bVW, _, fVScope, _, fVR, infoWriter, _) = createMockDSL[IO, VarSort]()

        val adt = MatchNormalizer.normalizeMatch[IO, VarSort](inputTerm).unsafeRunSync()

        val expectedAdt = MatchN(
          target = mockADT(targetTerm: Proc),
          cases = casesDataTerm.map { case (patternTerm, caseBodyTerm) =>
            MatchCaseN(
              pattern = mockADT(patternTerm: Proc),
              source = mockADT(caseBodyTerm: Proc),
            )
          },
        )

        adt shouldBe expectedAdt

        val terms = nRec.extractData

        // Expect all terms to be normalized in sequence
        val expectedTerms = TermData(ProcTerm(targetTerm)) +: casesDataTerm.flatMap {
          case (patternTerm, caseBodyTerm) =>
            Seq(
              TermData(term = ProcTerm(patternTerm), boundNewScopeLevel = 1, freeScopeLevel = 1, insidePattern = true),
              TermData(term = ProcTerm(caseBodyTerm), boundCopyScopeLevel = 1),
            )
        }
        terms shouldBe expectedTerms
    }
  }

  it should "bound free variables in copy scope level" in {
    forAll { (varsNameStr: Seq[String]) =>
      val cases     = List(new CaseImpl(new PNil, new PNil))
      val listCases = new ListCase()
      cases.foreach(listCases.add)
      // match Nil { case Nil => Nil}
      val term      = new PMatch(new PNil, listCases)

      val initFreeVars = varsNameStr.distinct.zipWithIndex.map { case (name, index) => (name, (index, ProcSort)) }.toMap

      implicit val (nRec, bVScope, bVW, _, fVScope, _, fVR, infoWriter, _) =
        createMockDSL[IO, VarSort](initFreeVars = initFreeVars)

      MatchNormalizer.normalizeMatch[IO, VarSort](term).unsafeRunSync()

      val addedBoundVars    = bVW.extractData
      val expectedBoundVars = initFreeVars.keys.toSeq.map(BoundVarWriterData(_, varType = ProcSort, copyScopeLevel = 1))

      addedBoundVars.toSet shouldBe expectedBoundVars.toSet
    }
  }
}
