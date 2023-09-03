package sdk.api

import sdk.api.data.BlockDeploys

trait BlockDeploysDbApi[F[_]] {
  def insert(blockDeploys: BlockDeploys): F[BlockDeploys]
  def getByBlock(blockId: Long): F[Seq[BlockDeploys]]
}
