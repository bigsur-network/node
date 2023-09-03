package sdk.api

import sdk.api.data.*

trait ExternalApi[F[_]] {
  def getBlockById(blockId: Long): F[Option[Block]]
  def getDeploysByBlockId(blockId: Long): F[Seq[Deploy]]
}
