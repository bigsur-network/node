package squeryl.api

import cats.effect.Sync
import cats.syntax.all.*
import sdk.api.DeployDbApi
import sdk.api.data.Deploy
import sdk.db.*
import squeryl.{withSession, SqlConn}
import squeryl.RhonixNodeDb.deployTable
import squeryl.tables.CustomTypeMode.*
import squeryl.tables.DeployTable

class DeployDbApiImpl[F[_]: Sync: SqlConn] extends DeployDbApi[F] {
  override def insert(deploy: Deploy): F[Long] =
    withSession(deployTable.insert(DeployTable.toDb(0L, deploy))).map(_.id)

  override def update(id: Long, deploy: Deploy): F[Unit] =
    withSession(deployTable.update(DeployTable.toDb(id, deploy)))

  override def getById(id: Long): F[Option[Deploy]] =
    withSession(deployTable.where(_.id === id).headOption.map(d => DeployTable.fromDb(d)))

  override def getByHash(hash: Array[Byte]): F[Option[Deploy]] =
    withSession(deployTable.where(_.hash === hash).headOption.map(d => DeployTable.fromDb(d)))
}

object DeployDbApiImpl {
  def apply[F[_]: DeployDbApiImpl]: DeployDbApiImpl[F] = implicitly[DeployDbApiImpl[F]]
}
