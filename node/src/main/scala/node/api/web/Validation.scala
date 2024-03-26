package node.api.web

import cats.data.ValidatedNel
import cats.syntax.all.*
import node.Node
import sdk.api.*
import sdk.api.data.TokenTransferRequest
import sdk.codecs.Digest
import sdk.crypto.{ECDSA, PubKey, Sig}
import sdk.hashing.Blake2b

object Validation {
  type ValidationResult[+A] = ValidatedNel[ApiErr, A]

  // Failure of decoding Base16 value should result in this constant.
  // Since JSON parsing is handled by endpoint4s, this is the trick to pass the information about
  // decode failure in the general validation pipeline.
  val InvalidBase16DecodeResult: Array[Byte] = Array.emptyByteArray

  def validateTokenTransferRequest(
    x: TokenTransferRequest,
  ) /*(implicit bodyDigest: Digest[TokenTransferRequest.Body])*/: ValidationResult[TokenTransferRequest] =
    (validateBase16Encoding(x.pubKey, "pubKey") *>
      validateBase16Encoding(x.digest, "digest") *>
      validateBase16Encoding(x.signature, "signature") *>
      validateBodyDigest(x.body, x.digest) *>
      validateSignature(x) *>
      validateTransferValue(x.body.value))
      .as(x)

  private def validateBase16Encoding(data: Array[Byte], fieldName: String): ValidationResult[Unit] =
    (!data.sameElements(InvalidBase16DecodeResult))
      .guard[Option]
      .toValidNel(Base16DecodingFailed(fieldName))

  private def validateBodyDigest(body: TokenTransferRequest.Body, digest: Array[Byte]) /*(implicit
    bodyDigest: Digest[TokenTransferRequest.Body],
  )*/: ValidationResult[Unit] =
    // bodyDigest.digest(body).bytes.sameElements(digest).guard[Option].toValidNel(BodyDigestIsInvalid)
    (digest.length == Blake2b.HashSize).guard[Option].toValidNel(BodyDigestIsInvalid)

  private def validateSignature(
    x: TokenTransferRequest,
  ): ValidationResult[Unit] = {
    val unknownSigErr = UnknownSignatureAlgorithm(x.signatureAlg)
    val wrongSigErr   = SignatureIsInvalid(x.signature, x.digest, x.pubKey, x.signatureAlg)

    val checkIfSigSupported             = Node.SupportedECDSA.get(x.signatureAlg).toValidNel(unknownSigErr)
    def checkIfSigCorrect(ecdsa: ECDSA) = ecdsa
      .verify(x.digest, new Sig(x.signature), new PubKey(x.pubKey))
      .toOption
      .flatMap(_.guard[Option])
      .toValidNel(wrongSigErr)
      .void

    checkIfSigSupported.void // andThen checkIfSigCorrect
  }

  private def validateTransferValue(value: Long): ValidationResult[Unit] =
    (value > 0).guard[Option].toValidNel(TransferValueIsInvalid)
}
