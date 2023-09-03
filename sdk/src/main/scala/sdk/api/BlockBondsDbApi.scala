package sdk.api

import sdk.api.data.BlockBonds

trait BlockBondsDbApi[F[_]] {
  def insert(blockBonds: BlockBonds): F[BlockBonds]
  def getByBlock(blockId: Long): F[Seq[BlockBonds]]
}
