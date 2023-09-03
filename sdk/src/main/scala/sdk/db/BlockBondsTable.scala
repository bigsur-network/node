package sdk.db

import sdk.api.data.*

@SuppressWarnings(Array("org.wartremover.warts.FinalCaseClass"))
case class BlockBondsTable(blockId: Long, bondId: Long)

object BlockBondsTable {
  def toDb(blockBonds: BlockBonds): BlockBondsTable   = BlockBondsTable(blockBonds.blockId, blockBonds.bondId)
  def fromDb(blockBonds: BlockBondsTable): BlockBonds = BlockBonds(blockBonds.blockId, blockBonds.bondId)
}

trait BlockBondsDbApi[F[_]] {
  def insert(blockBonds: BlockBonds): F[BlockBonds]
  def getByBlock(blockId: Long): F[Seq[BlockBonds]]
}
