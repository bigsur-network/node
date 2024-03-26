package io.rhonix.rholang.normalizer

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import coop.rchain.rholang.interpreter.compiler.VarSort
import io.rhonix.rholang.normalizer.util.Mock.*
import io.rhonix.rholang.normalizer.util.MockNormalizerRec.mockADT
import io.rhonix.rholang.ast.rholang.Absyn.*
import io.rhonix.rholang.types.{GBoolN, MatchCaseN, MatchN}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class IfElseNormalizerSpec extends AnyFlatSpec with ScalaCheckPropertyChecks with Matchers {

  behavior of "IfElse normalizer"

  "IfElse normalizer" should "normalize PIfElse term" in {
    forAll { (targetStr: String, trueCaseStr: String, falseCaseStr: String) =>
      val targetTerm = new PVar(new ProcVarVar(targetStr))
      val trueCase   = new PGround(new GroundString(trueCaseStr))
      val falseCase  = new PGround(new GroundString(falseCaseStr))
      val inputTerm  = new PIfElse(targetTerm, trueCase, falseCase)

      implicit val (nRec, _, _, _, _, _, _, _, _) = createMockDSL[IO, VarSort]()

      val adt = IfNormalizer.normalizeIfElse[IO](inputTerm).unsafeRunSync()

      val expectedAdt = MatchN(
        target = mockADT(targetTerm: Proc),
        cases = Seq(
          MatchCaseN(pattern = GBoolN(true), source = mockADT(trueCase: Proc)),
          MatchCaseN(pattern = GBoolN(false), source = mockADT(falseCase: Proc)),
        ),
      )

      adt shouldBe expectedAdt

      val terms = nRec.extractData

      val expectedTerms = Seq(
        TermData(ProcTerm(targetTerm)),
        TermData(ProcTerm(trueCase)),
        TermData(ProcTerm(falseCase)),
      )
      terms shouldBe expectedTerms
    }
  }
}
