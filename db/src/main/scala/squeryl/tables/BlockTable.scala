package squeryl.tables

import sdk.api.data.*

@SuppressWarnings(Array("org.wartremover.warts.FinalCaseClass"))
case class BlockTable(
  id: Long,
  hash: Array[Byte],
  senderId: Long,
  version: Int,
  shardId: String,
  seqNum: Long,
  number: Long,
  justificationsHash: Array[Byte], // hash(Set[Justification]),
  bondsHash: Array[Byte],          // hash(Map[Validator, Long]),

  // Rholang (tuple space) state change
  preStateHash: Array[Byte],  // hash(VM state)
  postStateHash: Array[Byte], // hash(VM state)
  deploysHash: Array[Byte],   // hash(Set[Deploy])

  // Block signature
  signatureAlg: String,
  signature: Array[Byte],

  // Status
  status: Int,
) extends DbTable

object BlockTable {
  def toDb(id: Long, block: Block, senderId: Long): BlockTable = BlockTable(
    id,
    block.hash,
    senderId,
    block.version,
    block.shardId,
    block.seqNum,
    block.number,
    block.justificationsHash,
    block.bondsHash,
    block.preStateHash,
    block.postStateHash,
    block.deploysHash,
    block.signatureAlg,
    block.signature,
    block.status,
  )

  def fromDb(
    block: BlockTable,
    sender: Validator,
    justifications: Set[Validator],
    bonds: Set[Bond],
    deploys: Set[Deploy],
  ): Block = Block(
    block.hash,
    sender,
    block.version,
    block.shardId,
    block.seqNum,
    block.number,
    block.justificationsHash,
    justifications,
    block.bondsHash,
    bonds,
    block.preStateHash,
    block.postStateHash,
    block.deploysHash,
    deploys,
    block.signatureAlg,
    block.signature,
    block.status,
  )
}
