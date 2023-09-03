package sdk.api

import sdk.api.data.Deploy

trait DeployDbApi[F[_]] {
  def insert(deploy: Deploy): F[Long]
  def update(id: Long, deploy: Deploy): F[Unit]

  def getById(id: Long): F[Option[Deploy]]
  def getByHash(hash: Array[Byte]): F[Option[Deploy]]
}
