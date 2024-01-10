package sdk.crypto

/** Splittable pseudorandom number generator. */
trait SplittablePrng[A] {
  def splitByte(x: A, index: Byte): A
  def splitShort(x: A, index: Short): A
  def next(x: A): Array[Byte]
  def copy(x: A): A
}

object SplittablePrng {
  def apply[A](implicit x: SplittablePrng[A]): SplittablePrng[A] = x
}
