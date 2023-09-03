package squeryl.tables

import sdk.api.data.BlockJustifications

@SuppressWarnings(Array("org.wartremover.warts.FinalCaseClass"))
case class BlockJustificationsTable(validatorId: Long, latestBlockId: Long)

object BlockJustificationsTable {
  def toDb(blockJustifications: BlockJustifications): BlockJustificationsTable   =
    BlockJustificationsTable(blockJustifications.validatorId, blockJustifications.latestBlockId)
  def fromDb(blockJustifications: BlockJustificationsTable): BlockJustifications =
    BlockJustifications(blockJustifications.validatorId, blockJustifications.latestBlockId)
}
