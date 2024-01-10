package sdk.crypto

import org.scalacheck.{Arbitrary, Gen}

package object blake2rnd {

  implicit val arbitraryBlake2b512Block: Arbitrary[Blake2b512Block] = Arbitrary(for {
    chainValue <- Gen.containerOfN[Array, Long](8, Arbitrary.arbitrary[Long])
    t0         <- Arbitrary.arbitrary[Long]
    t1         <- Arbitrary.arbitrary[Long]
  } yield {
    val result = new Blake2b512Block(chainValue, t0, t1)
    result
  })

  implicit val arbitraryBlake2b512Random: Arbitrary[Blake2b512Random] = Arbitrary(for {
    bytes <- Gen.containerOfN[Array, Byte](32, Arbitrary.arbitrary[Byte])
  } yield Blake2b512Random(bytes))

  // Why this complex Arbitrary has been used before?
//  implicit val arbitraryBlake2b512Random: Arbitrary[Blake2b512Random] = Arbitrary(for {
//    digest       <- Arbitrary.arbitrary[Blake2b512Block]
//    position     <- Gen.oneOf[Int](0, 32)
//    // This only works at 0 and 32.
//    remainder    <- Gen.containerOfN[Array, Byte](position, Arbitrary.arbitrary[Byte])
//    countLow     <- Arbitrary.arbitrary[Long]
//    countHigh    <- Arbitrary.arbitrary[Long]
//    pathPosition <- Gen.choose[Int](0, 112)
//    path         <- Gen.containerOfN[Array, Byte](pathPosition, Arbitrary.arbitrary[Byte])
//  } yield {
//    val result = new Blake2b512Random(digest, ByteBuffer.allocate(128))
//    result.countView.put(0, countLow)
//    result.countView.put(1, countHigh)
//    result.pathView.put(path)
//    if (position != 0)
//      Array.copy(remainder, 0, result.hashArray, position, 64 - position)
//    result.position = position
//    result
//  })
}
