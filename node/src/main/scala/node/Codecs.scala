package node

import cats.Eval
import sdk.api.data.Balance
import sdk.codecs.*
import sdk.codecs.protobuf.*
import sdk.primitive.ByteArray
import sdk.serialize.auto.*

import scala.util.Try

/** Implementation of the binary protocol. */
object Codecs {
  val ser: Serialize[Eval, Balance] = implicitly[Serialize[Eval, Balance]]

  implicit val balanceCodec: Codec[Balance, ByteArray] = new Codec[Balance, ByteArray] {
    override def encode(x: Balance): Try[ByteArray] = Try {
      ProtoPrimitiveWriter.encodeWith(ser.write(x)).map(ByteArray(_)).value
    }
    override def decode(x: ByteArray): Try[Balance] = Try {
      ProtoPrimitiveReader.decodeWith(x, ser.read).value
    }
  }
}
