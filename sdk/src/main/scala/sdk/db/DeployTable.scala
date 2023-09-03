package sdk.db

import sdk.DbTable
import sdk.api.data.Deploy

@SuppressWarnings(Array("org.wartremover.warts.FinalCaseClass"))
case class DeployTable(
  id: Long,
  hash: Array[Byte],
  publicKey: Array[Byte],
  shardId: String,
  program: String,
  phloPrice: Long,
  phloLimit: Long,
  timestamp: Long,
  validAfterBlockNumber: Long,
) extends DbTable

object DeployTable {
  def toDb(id: Long, deploy: Deploy): DeployTable = DeployTable(
    id,
    deploy.hash,
    deploy.publicKey,
    deploy.shardId,
    deploy.program,
    deploy.phloPrice,
    deploy.phloLimit,
    deploy.timestamp,
    deploy.validAfterBlockNumber,
  )

  def fromDb(deploy: DeployTable): Deploy = Deploy(
    deploy.hash,
    deploy.publicKey,
    deploy.shardId,
    deploy.program,
    deploy.phloPrice,
    deploy.phloLimit,
    deploy.timestamp,
    deploy.validAfterBlockNumber,
  )
}

trait DeployDbApi[F[_]] {
  def insert(deploy: Deploy): F[Long]
  def update(id: Long, deploy: Deploy): F[Unit]

  def getById(id: Long): F[Option[Deploy]]
  def getByHash(hash: Array[Byte]): F[Option[Deploy]]
}
