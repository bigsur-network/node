package sdk.api.data

import cats.kernel.Eq

final case class Block(
  hash: Array[Byte],
  sender: Array[Byte],
  version: Int,
  shardId: String,
  seqNum: Long,
  number: Long,
  justifications: Set[Array[Byte]],
  bonds: Set[Bond],

  // Rholang (tuple space) state change
  finStateHash: Array[Byte],  // hash(VM state)
  preStateHash: Array[Byte],  // hash(VM state)
  postStateHash: Array[Byte], // hash(VM state)

  deploys: Set[Array[Byte]],

  // Block signature
  signatureAlg: String,
  signature: Array[Byte],

  // Status
  status: Int,
) {
  override def equals(obj: Any): Boolean = obj match {
    case that: Block => this.hash sameElements that.hash
    case _           => false
  }

  override def hashCode(): Int = hash.hashCode()
}

object Block {
  implicit val blockEq: Eq[Block] = Eq.fromUniversalEquals

  def apply(b: sdk.data.Block): Block = new Block(
    hash = b.hash.bytes,
    sender = b.validatorPk.bytes,
    version = b.version,
    shardId = b.shardName,
    seqNum = b.seqNum,
    number = b.seqNum,
    justifications = b.justificationSet.map(_.bytes),
    bonds = b.bondsMap.map { case (k, v) => Bond(k.bytes, v) }.toSet,
    finStateHash = b.finalStateHash.bytes,
    preStateHash = b.finalStateHash.bytes,
    postStateHash = b.postStateHash.bytes,
    deploys = b.execDeploySet.map(_.bytes),
    signatureAlg = b.sigAlg,
    signature = b.signature.bytes,
    status = 0,
  )
}
