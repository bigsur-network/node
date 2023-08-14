package sdk.dag

trait SeqNum[T] {
  def seqNum(x: T): Int
}
