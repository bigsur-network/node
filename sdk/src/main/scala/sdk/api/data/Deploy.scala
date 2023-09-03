package sdk.api.data

import cats.kernel.Eq

final case class Deploy(
  hash: Array[Byte],
  publicKey: Array[Byte],
  shardId: String,
  program: String,
  phloPrice: Long,
  phloLimit: Long,
  timestamp: Long,
  validAfterBlockNumber: Long,
) {

  override def equals(obj: Any): Boolean = obj match {
    case that: Deploy => this.hash.sameElements(that.hash)
    case _            => false
  }

  override def hashCode(): Int = hash.hashCode()
}

object Deploy {
  implicit val deployEq: Eq[Deploy] = Eq.fromUniversalEquals

  // Statuses
  val DEPLOY_POOLED: Int             = 0
  val DEPLOY_PROPOSED: Int           = 1
  val DEPLOY_FINALIZED_ACCEPTED: Int = 2
  val DEPLOY_FINALIZED_REJECTED: Int = 3
}
