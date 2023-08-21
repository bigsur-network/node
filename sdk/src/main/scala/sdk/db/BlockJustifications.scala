package sdk.db

@SuppressWarnings(Array("org.wartremover.warts.FinalCaseClass"))
case class BlockJustificationsTable(validatorId: Long, latestBlockId: Long)

final case class BlockJustifications(validatorId: Long, latestBlockId: Long)

object BlockJustifications {
  def toDb(blockJustifications: BlockJustifications): BlockJustificationsTable   =
    BlockJustificationsTable(blockJustifications.validatorId, blockJustifications.latestBlockId)
  def fromDb(blockJustifications: BlockJustificationsTable): BlockJustifications =
    BlockJustifications(blockJustifications.validatorId, blockJustifications.latestBlockId)
}

trait BlockJustificationsDbApi[F[_]] {
  def insert(blockJustifications: BlockJustifications): F[BlockJustifications]
  def getByBlock(latestBlockId: Long): F[Seq[BlockJustifications]]
}
