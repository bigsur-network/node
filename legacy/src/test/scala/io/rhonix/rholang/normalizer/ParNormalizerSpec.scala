package io.rhonix.rholang.normalizer

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import coop.rchain.rholang.interpreter.compiler.VarSort
import io.rhonix.rholang.normalizer.util.Mock.*
import io.rhonix.rholang.normalizer.util.MockNormalizerRec.mockADT
import io.rhonix.rholang.ast.rholang.Absyn.{GroundString, PGround, PPar, Proc}
import io.rhonix.rholang.types.ParProcN
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class ParNormalizerSpec extends AnyFlatSpec with ScalaCheckPropertyChecks with Matchers {

  "Par normalizer" should "normalize PPar term" in {
    forAll { (s1: String, s2: String) =>
      val left  = new PGround(new GroundString(s1))
      val right = new PGround(new GroundString(s2))
      // term1 | term2
      val term  = new PPar(left, right)

      implicit val (nRec, _, _, _, _, _, _, _, _) = createMockDSL[IO, VarSort]()

      // Run Par normalizer
      val adt = ParNormalizer.normalizePar[IO](term).unsafeRunSync()

      // Expect right converted ADT term
      val expectedAdt = ParProcN(Seq(mockADT(left: Proc), mockADT(right: Proc)))

      adt shouldBe expectedAdt

      val terms         = nRec.extractData
      // Expect both sides of par to be normalized in sequence
      val expectedTerms = Seq(TermData(ProcTerm(left)), TermData(ProcTerm(right)))

      terms shouldBe expectedTerms
    }
  }
}
