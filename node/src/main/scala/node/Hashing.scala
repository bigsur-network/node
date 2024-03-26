package node

import cats.Eval
import sdk.codecs.*
import sdk.codecs.protobuf.ProtoPrimitiveWriter
import sdk.hashing.Blake2b
import sdk.history.ByteArray32
import sdk.primitive.ByteArray
import sdk.syntax.all.*

object Hashing {

  // For hashing Blake2b is used
  implicit def hash(x: Array[Byte]): ByteArray32 = ByteArray32.convert(Blake2b.hash256(x)).getUnsafe

  implicit def digest[A](implicit ser: Serialize[Eval, A]): Digest[A] =
    new Digest[A] {
      override def digest(x: A): ByteArray = {
        val bytes = ProtoPrimitiveWriter.encodeWith(ser.write(x))
        ByteArray(Blake2b.hash256(bytes.value))
      }
    }
}
