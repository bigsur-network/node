package sim.api.data

import cats.data.Validated.Valid
import node.Node
import node.api.web.Validation
import org.scalatest.Assertion
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sdk.api.*
import sdk.api.data.TokenTransferRequest
import sdk.codecs.Digest
import sdk.crypto.ECDSA
import secp256k1.Secp256k1
import node.codec.Hashing.*

class TokenTransferValidationSpec extends AnyFlatSpec with Matchers {

  "Correct request data" should "produce correct TokenTransferRequest instance" in {
    val tx = makeRequestData
    Validation.validateTokenTransferRequest(tx) shouldBe Valid(tx)
  }

  "Incorrect base16 strings" should "produce Base16DecodingFailed error" in {
    val tx               = makeRequestData.copy(
      pubKey = Validation.InvalidBase16DecodeResult,
      digest = Validation.InvalidBase16DecodeResult,
      signature = Validation.InvalidBase16DecodeResult,
    )
    val validationResult = Validation.validateTokenTransferRequest(tx)
    checkErrorCount(validationResult, _.isInstanceOf[Base16DecodingFailed], 3)
  }

  "Incorrect body digest" should "produce BodyDigestIsInvalid error" in {
    val tx               = makeRequestData.copy(digest = Array(1.toByte))
    val validationResult = Validation.validateTokenTransferRequest(tx)
    checkErrorCount(validationResult, _.isInstanceOf[BodyDigestIsInvalid.type], 1)
  }

  "Incorrect signature algorithm" should "produce UnknownSignatureAlgorithm error" in {
    val tx               = makeRequestData.copy(signatureAlg = "sect113r1")
    val validationResult = Validation.validateTokenTransferRequest(tx)
    checkErrorCount(validationResult, _.isInstanceOf[UnknownSignatureAlgorithm], 1)
  }

  "Incorrect signature" should "produce SignatureIsInvalid error" in {
    val tx               = makeRequestData.copy(signature = Array(1.toByte))
    val validationResult = Validation.validateTokenTransferRequest(tx)
    checkErrorCount(validationResult, _.isInstanceOf[SignatureIsInvalid], 1)
  }

  "Incorrect transfer value" should "produce TransferValueIsInvalid error" in {
    val tx               = makeRequestData
    val txWithWrongValue = tx.copy(body = tx.body.copy(value = 0))
    val validationResult = Validation.validateTokenTransferRequest(txWithWrongValue)
    checkErrorCount(validationResult, _.isInstanceOf[TransferValueIsInvalid.type], 1)
  }

  private def makeRequestData: TokenTransferRequest = {
    val body       = TokenTransferRequest.Body(Array.empty[Byte], Array.empty[Byte], 0L, 1L, 0L)
    val digest     = implicitly[Digest[TokenTransferRequest.Body]].digest(body).bytes
    val sigAlgName = Node.SupportedECDSA.head._1
    val sigAlg     = Node.SupportedECDSA.head._2
    val (sec, pub) = sigAlg.newKeyPair
    val signature  = sigAlg.sign(digest, sec).map(_.value).getOrElse(Array.empty[Byte])
    TokenTransferRequest(pub.value, digest, signature, sigAlgName, body)
  }

  private def checkErrorCount[E <: ApiErr](
    validationResult: Validation.ValidationResult[TokenTransferRequest],
    checkErrorType: ApiErr => Boolean,
    count: Int,
  ): Assertion =
    validationResult.fold(_.toList.count(checkErrorType), _ => 0) shouldBe count
}
