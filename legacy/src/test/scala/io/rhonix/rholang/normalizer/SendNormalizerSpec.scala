package io.rhonix.rholang.normalizer

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import coop.rchain.rholang.interpreter.compiler.VarSort
import io.rhonix.rholang.normalizer.util.Mock.*
import io.rhonix.rholang.normalizer.util.MockNormalizerRec.mockADT
import io.rhonix.rholang.ast.rholang.Absyn.*
import io.rhonix.rholang.types.SendN
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class SendNormalizerSpec extends AnyFlatSpec with ScalaCheckPropertyChecks with Matchers {

  "Send normalizer" should "normalize PSend term" in {
    forAll { (chanStr: String, argsStr: Seq[String], persistent: Boolean) =>
      val chan     = new NameVar(chanStr)
      val sendType = if (persistent) new SendMultiple() else new SendSingle()

      val args = argsStr.map(x => new PGround(new GroundString(x)))

      val listArgs = new ListProc()
      args.foreach(listArgs.add)

      val term = new PSend(chan, sendType, listArgs)

      implicit val (nRec, _, _, _, _, _, _, _, _) = createMockDSL[IO, VarSort]()

      val adt = SendNormalizer.normalizeSend[IO](term).unsafeRunSync()

      val expectedAdt = SendN(chan = mockADT(chan), args = args.map(mockADT), persistent = persistent)

      adt shouldBe expectedAdt
    }
  }
}
