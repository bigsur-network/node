package io.rhonix.rholang.normalizer

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import coop.rchain.rholang.interpreter.compiler.VarSort
import io.rhonix.rholang.normalizer.util.Mock.*
import io.rhonix.rholang.normalizer.util.MockNormalizerRec.mockADT
import io.rhonix.rholang.ast.rholang.Absyn.{GroundString, ListProc, PGround, PMethod, Proc}
import io.rhonix.rholang.types.EMethodN
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class MethodNormalizerSpec extends AnyFlatSpec with ScalaCheckPropertyChecks with Matchers {

  "Method normalizer" should "normalize PMethod term" in {
    forAll { (targetStr: String, methodName: String, argsStr: Seq[String]) =>
      val target = new PGround(new GroundString(targetStr))

      val args     = argsStr.map(s => new PGround(new GroundString(s)))
      val argsList = new ListProc
      args.foreach(argsList.add)

      // target.methodName(args)
      val term = new PMethod(target, methodName, argsList)

      implicit val (nRec, _, _, _, _, _, _, _, _) = createMockDSL[IO, VarSort]()

      val adt         = MethodNormalizer.normalizeMethod[IO](term).unsafeRunSync()
      val expectedAdt = EMethodN(target = mockADT(target: Proc), methodName = methodName, args = args.map(mockADT))
      adt shouldBe expectedAdt

      val terms         = nRec.extractData
      val expectedTerms = TermData(ProcTerm(target)) +: args.map(x => TermData(ProcTerm(x)))
      terms shouldBe expectedTerms
    }
  }
}
