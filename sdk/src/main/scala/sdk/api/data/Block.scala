package sdk.api.data

import cats.kernel.Eq

final case class Block(
  hash: Array[Byte],
  sender: Validator,
  version: Int,
  shardId: String,
  seqNum: Long,
  number: Long,
  justificationsHash: Array[Byte], // hash(Set[Justification]),
  justifications: Set[Validator],
  bondsHash: Array[Byte],          // hash(Map[Validator, Long]),
  bonds: Set[Bond],

  // Rholang (tuple space) state change
  preStateHash: Array[Byte],  // hash(VM state)
  postStateHash: Array[Byte], // hash(VM state)
  deploysHash: Array[Byte],   // hash(Set[Deploy])
  deploys: Set[Deploy],

  // Block signature
  signatureAlg: String,
  signature: Array[Byte],

  // Status
  status: Int,
) {
  override def equals(obj: Any): Boolean = obj match {
    case that: Block => this.hash.sameElements(that.hash)
    case _           => false
  }

  override def hashCode(): Int = hash.hashCode()
}

object Block {
  implicit val blockEq: Eq[Block] = Eq.fromUniversalEquals
}
