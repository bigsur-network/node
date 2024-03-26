package io.rhonix.rholang.normalizer

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import coop.rchain.rholang.interpreter.errors.NormalizerError
import io.rhonix.rholang.ast.rholang.Absyn.*
import io.rhonix.rholang.types.{GBigIntN, GBoolN, GIntN, GStringN, GUriN}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class GroundNormalizerSpec extends AnyFlatSpec with ScalaCheckPropertyChecks with Matchers {

  behavior of "Ground bool normalizer"

  it should "return the Rholang boolean for input boolean term" in {
    forAll { (booleanData: Boolean) =>
      val boolLiteral = if (booleanData) new BoolTrue() else new BoolFalse()
      val term        = new PGround(new GroundBool(boolLiteral))

      val par = GroundNormalizer.normalizeGround[IO](term).unsafeRunSync()
      par shouldBe GBoolN(booleanData)
    }
  }

  behavior of "Ground int normalizer"

  it should "return the Rholang integer for input integer term" in {
    forAll { (intData: Long) =>
      val term = new PGround(new GroundInt(intData.toString))

      val par = GroundNormalizer.normalizeGround[IO](term).unsafeRunSync()
      par shouldBe GIntN(intData)
    }
  }

  it should "throw an exception if the integer is too big to fit in a long" in {
    val badValue = "9223372036854775808" // 2^63 + 1
    val term     = new PGround(new GroundInt(badValue))
    val thrown   = intercept[NormalizerError] {
      GroundNormalizer.normalizeGround[IO](term).unsafeRunSync()
    }
    thrown.getMessage should include(badValue)
  }

  it should "throw an exception if the integer is too small to fit in a long" in {
    val badValue = "-9223372036854775809" // -2^63 - 1
    val term     = new PGround(new GroundInt(badValue))
    val thrown   = intercept[NormalizerError] {
      GroundNormalizer.normalizeGround[IO](term).unsafeRunSync()
    }
    thrown.getMessage should include(badValue)
  }

  it should "throw an exception if the integer is not a valid number" in {
    val badValue = "string"
    val term     = new PGround(new GroundInt(badValue))
    val thrown   = intercept[NormalizerError] {
      GroundNormalizer.normalizeGround[IO](term).unsafeRunSync()
    }
    thrown.getMessage should include(badValue)
  }

  behavior of "Ground int normalizer"

  it should "return the Rholang big integer for input big integer term" in {
    forAll { (bigIntData: BigInt) =>
      val term = new PGround(new GroundBigInt(bigIntData.toString))

      val par = GroundNormalizer.normalizeGround[IO](term).unsafeRunSync()
      par shouldBe GBigIntN(bigIntData)
    }
  }

  it should "throw an exception if the big integer is not a valid number" in {
    val badValue = "string"
    val term     = new PGround(new GroundBigInt(badValue))
    val thrown   = intercept[NormalizerError] {
      GroundNormalizer.normalizeGround[IO](term).unsafeRunSync()
    }
    thrown.getMessage should include(badValue)
  }

  behavior of "Ground string normalizer"

  it should "return the Rholang string for input string term" in {
    forAll { (stringData: String) =>
      // String data should be wrapped in backticks
      val term = new PGround(new GroundString(s"`$stringData`"))

      val par = GroundNormalizer.normalizeGround[IO](term).unsafeRunSync()
      par shouldBe GStringN(stringData)
    }
  }

  it should "throw an exception if the length of input string shorter than 2" in {
    val inputString = "a"
    val term        = new PGround(new GroundString(inputString))
    val thrown      = intercept[IllegalArgumentException] {
      GroundNormalizer.normalizeGround[IO](term).unsafeRunSync()
    }
    thrown.getMessage should include(inputString)
  }

  behavior of "Ground URI normalizer"

  it should "return the Rholang URI for input URI term" in {
    forAll { (uriData: String) =>
      // Uri data should should be wrapped in backticks
      val term = new PGround(new GroundUri(s"`$uriData`"))

      val par = GroundNormalizer.normalizeGround[IO](term).unsafeRunSync()
      par shouldBe GUriN(uriData)
    }
  }

  it should "throw an exception if the length of input string shorter than 2" in {
    val inputUri = "a"
    val term     = new PGround(new GroundUri(inputUri))
    val thrown   = intercept[IllegalArgumentException] {
      GroundNormalizer.normalizeGround[IO](term).unsafeRunSync()
    }
    thrown.getMessage should include(inputUri)
  }
}
