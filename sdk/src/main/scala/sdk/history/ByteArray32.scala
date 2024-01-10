package sdk.history

import sdk.codecs.Codec
import sdk.primitive.ByteArray
import sdk.syntax.all.*

import scala.util.Try

final case class ByteArray32 private (bytes: ByteArray) {

  require(
    bytes.length == ByteArray32.Length,
    s"Expected ${ByteArray32.Length} but got ${bytes.length}",
  )

  override def toString: String = s"Blake(${bytes.toHex})"
}

object ByteArray32 {

  implicit val ordering: Ordering[ByteArray32] = Ordering.by(_.bytes)

  val Length: Int = 32

  def convert(bytes: ByteArray): Try[ByteArray32]   = Try {
    assert(s"${bytes.length}" == s"${Length}", s"Cannot create ByteArray32 from byte array of size ${bytes.length}")
    new ByteArray32(bytes)
  }
  def convert(bytes: Array[Byte]): Try[ByteArray32] = convert(ByteArray(bytes))

  def codec: Codec[ByteArray32, ByteArray] = new Codec[ByteArray32, ByteArray] {
    override def encode(x: ByteArray32): Try[ByteArray] = Try(x.bytes)
    override def decode(x: ByteArray): Try[ByteArray32] = ByteArray32.convert(x)
  }
}
