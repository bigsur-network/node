package squeryl.api

import cats.effect.Sync
import sdk.api.ValidatorDbApi
import sdk.api.data.Validator
import squeryl.RhonixNodeDb.validatorTable
import squeryl.tables.CustomTypeMode.*
import squeryl.tables.ValidatorTable
import squeryl.{withSession, SqlConn}
import cats.syntax.all.*

class ValidatorDbApiImpl[F[_]: Sync: SqlConn] extends ValidatorDbApi[F] {
  override def insert(validator: Validator): F[Long] =
    withSession(validatorTable.insert(ValidatorTable.toDb(0L, validator))).map(_.id)

  override def update(id: Long, validator: Validator): F[Unit] =
    withSession(validatorTable.update(ValidatorTable.toDb(id, validator)))

  override def getById(id: Long): F[Option[Validator]] =
    withSession(validatorTable.where(_.id === id).headOption.map(v => ValidatorTable.fromDb(v)))

  override def getByPublicKey(publicKey: Array[Byte]): F[Option[Validator]] =
    withSession(validatorTable.where(_.publicKey === publicKey).headOption.map(v => ValidatorTable.fromDb(v)))
}

object ValidatorDbApiImpl {
  def apply[F[_]: ValidatorDbApiImpl]: ValidatorDbApiImpl[F] = implicitly[ValidatorDbApiImpl[F]]
}
