package sdk.api

import sdk.api.data.Block

trait BlockDbApi[F[_]] {
  def insert(block: Block, senderId: Long): F[Long]
  def update(id: Long, block: Block, senderId: Long): F[Unit]

  def getById(id: Long): F[Option[Block]]
  def getByHash(hash: Array[Byte]): F[Option[Block]]
}
