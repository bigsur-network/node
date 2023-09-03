package sdk.api

import sdk.api.data.Bond

trait BondDbApi[F[_]] {
  def insert(bond: Bond, validatorId: Long): F[Long]
  def update(id: Long, bond: Bond, validatorId: Long): F[Unit]

  def getById(id: Long): F[Option[Bond]]
}
