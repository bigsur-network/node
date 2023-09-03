package sdk.api

import sdk.api.data.BlockJustifications

trait BlockJustificationsDbApi[F[_]] {
  def insert(blockJustifications: BlockJustifications): F[BlockJustifications]
  def getByBlock(latestBlockId: Long): F[Seq[BlockJustifications]]
}
