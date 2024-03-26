package sdk.primitive

import sdk.codecs.Base16

import scala.util.Try

trait StringSyntax {
  implicit final def stringSyntax(s: String): StringOps =
    new StringOps(s)
}

class StringOps(private val s: String) extends AnyVal {
  def decodeHex: Try[Array[Byte]] = Base16.decode(s)
}
