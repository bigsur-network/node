package squeryl

import cats.effect.Sync
import cats.syntax.all.*
import sdk.api.ValidatorDbApi
import sdk.api.data.Validator
import sdk.db.DbSession
import sdk.db.DbSession.withSessionF
import squeryl.RhonixNodeDb.validatorTable
import squeryl.tables.CustomTypeMode.*
import squeryl.tables.ValidatorTable

class ValidatorDbApiImpl[F[_]: Sync: DbSession] extends ValidatorDbApi[F] {
  override def insert(validator: Validator): F[Long] =
    withSessionF(validatorTable.insert(ValidatorTable.toDb(0L, validator))).map(_.id)

  override def update(id: Long, validator: Validator): F[Unit] =
    withSessionF(validatorTable.update(ValidatorTable.toDb(id, validator)))

  override def getById(id: Long): F[Option[Validator]] =
    withSessionF(validatorTable.where(_.id === id).headOption.map(v => ValidatorTable.fromDb(v)))

  override def getByPublicKey(publicKey: Array[Byte]): F[Option[Validator]] =
    withSessionF(validatorTable.where(_.publicKey === publicKey).headOption.map(v => ValidatorTable.fromDb(v)))
}

object ValidatorDbApiImpl {
  def apply[F[_]: ValidatorDbApiImpl]: ValidatorDbApiImpl[F] = implicitly[ValidatorDbApiImpl[F]]
}
