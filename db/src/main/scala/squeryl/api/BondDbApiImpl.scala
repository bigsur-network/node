package squeryl.api

import cats.data.OptionT
import cats.effect.Sync
import cats.syntax.all.*
import sdk.api.BondDbApi
import sdk.api.data.Bond

import java.sql.Connection
import squeryl.{withSession, SqlConn}
import squeryl.RhonixNodeDb.{bondTable, validatorTable}
import squeryl.tables.BondTable
import squeryl.tables.CustomTypeMode.*

class BondDbApiImpl[F[_]: Sync: SqlConn] extends BondDbApi[F] {
  override def insert(bond: Bond, validatorId: Long): F[Long] =
    withSession(bondTable.insert(BondTable.toDb(0L, bond, validatorId))).map(_.id)

  override def update(id: Long, bond: Bond, validatorId: Long): F[Unit] =
    withSession(bondTable.update(BondTable.toDb(id, bond, validatorId)))

  override def getById(id: Long): F[Option[Bond]] =
    (for {
      bond      <- OptionT(withSession(bondTable.where(_.id === id).headOption))
      validator <- OptionT(withSession(validatorTable.where(_.id === bond.validatorId).headOption))
    } yield BondTable.fromDb(bond, validator)).value
}

object BondDbApiImpl {
  def apply[F[_]: BondDbApiImpl]: BondDbApiImpl[F] = implicitly[BondDbApiImpl[F]]
}
