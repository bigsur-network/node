package squeryl

import cats.effect.Sync
import cats.syntax.all.*
import sdk.api.DeployDbApi
import sdk.api.data.Deploy
import sdk.db.*
import sdk.db.DbSession.withSessionF
import squeryl.RhonixNodeDb.deployTable
import squeryl.tables.CustomTypeMode.*
import squeryl.tables.DeployTable

class DeployDbApiImpl[F[_]: Sync: DbSession] extends DeployDbApi[F] {
  override def insert(deploy: Deploy): F[Long] =
    withSessionF(deployTable.insert(DeployTable.toDb(0L, deploy))).map(_.id)

  override def update(id: Long, deploy: Deploy): F[Unit] =
    withSessionF(deployTable.update(DeployTable.toDb(id, deploy)))

  override def getById(id: Long): F[Option[Deploy]] =
    withSessionF(deployTable.where(_.id === id).headOption.map(d => DeployTable.fromDb(d)))

  override def getByHash(hash: Array[Byte]): F[Option[Deploy]] =
    withSessionF(deployTable.where(_.hash === hash).headOption.map(d => DeployTable.fromDb(d)))
}

object DeployDbApiImpl {
  def apply[F[_]: DeployDbApiImpl]: DeployDbApiImpl[F] = implicitly[DeployDbApiImpl[F]]
}
