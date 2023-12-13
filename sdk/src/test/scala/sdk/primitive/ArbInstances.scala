package sdk.primitive

import org.scalacheck.Arbitrary

trait ArbInstances {
  implicit def arbByteArray: Arbitrary[ByteArray] = Arbitrary {
    Arbitrary.arbitrary[Array[Byte]].map(ByteArray(_))
  }
}
