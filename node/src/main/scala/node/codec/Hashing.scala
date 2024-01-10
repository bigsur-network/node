package node.codec

import cats.Eval
import dproc.data.Block
import sdk.api.data.{Balance, TokenTransferRequest}
import sdk.codecs.Digest
import sdk.codecs.protobuf.ProtoPrimitiveWriter
import sdk.data.{BalancesDeploy, BalancesDeployBody}
import sdk.hashing.Blake2b
import sdk.history.ByteArray32
import sdk.primitive.ByteArray
import sdk.syntax.all.sdkSyntaxTry

object Hashing {
  implicit def blake2b256Hash(x: Array[Byte]): ByteArray32 = ByteArray32.convert(Blake2b.hash256(x)).getUnsafe

  implicit val balancesDeployBodyDigest: Digest[BalancesDeployBody] =
    new sdk.codecs.Digest[BalancesDeployBody] {
      override def digest(x: BalancesDeployBody): ByteArray = {
        val bytes = ProtoPrimitiveWriter.encodeWith(Serialization.balancesDeployBodySerialize[Eval].write(x))
        ByteArray(Blake2b.hash256(bytes.value))
      }
    }

  implicit val balancesDigest: Digest[Balance] =
    new sdk.codecs.Digest[Balance] {
      override def digest(x: Balance): ByteArray = {
        val bytes = ProtoPrimitiveWriter.encodeWith(Serialization.balanceSerialize[Eval].write(x))
        ByteArray(Blake2b.hash256(bytes.value))
      }
    }

  implicit val blockBodyDigest: Digest[Block[ByteArray, ByteArray, BalancesDeploy]] =
    new sdk.codecs.Digest[Block[ByteArray, ByteArray, BalancesDeploy]] {
      override def digest(x: Block[ByteArray, ByteArray, BalancesDeploy]): ByteArray = {
        val bytes = ProtoPrimitiveWriter.encodeWith(Serialization.blockSerialize[Eval].write(x))
        ByteArray(Blake2b.hash256(bytes.value))
      }
    }

  implicit val tokenTransferRequestBodyDigest: Digest[TokenTransferRequest.Body] =
    new sdk.codecs.Digest[TokenTransferRequest.Body] {
      override def digest(x: TokenTransferRequest.Body): ByteArray = {
        val bytes = ProtoPrimitiveWriter.encodeWith(Serialization.tokenTransferRequestBodySerialize[Eval].write(x))
        ByteArray(Blake2b.hash256(bytes.value))
      }
    }
}
