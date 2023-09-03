package sdk.api

import sdk.api.data.Validator

trait ValidatorDbApi[F[_]] {
  def insert(validator: Validator): F[Long]
  def update(id: Long, validator: Validator): F[Unit]

  def getById(id: Long): F[Option[Validator]]
  def getByPublicKey(publicKey: Array[Byte]): F[Option[Validator]]
}
