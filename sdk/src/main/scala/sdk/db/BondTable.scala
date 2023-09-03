package sdk.db

import sdk.DbTable
import sdk.api.data.Bond

@SuppressWarnings(Array("org.wartremover.warts.FinalCaseClass"))
case class BondTable(
  id: Long,
  validatorId: Long,
  stake: Long,
) extends DbTable

object BondTable {
  def toDb(id: Long, bond: Bond, validatorId: Long): BondTable = BondTable(id, validatorId, bond.stake)
  def fromDb(bond: BondTable, validator: ValidatorTable): Bond = Bond(ValidatorTable.fromDb(validator), bond.stake)
}

trait BondDbApi[F[_]] {
  def insert(bond: Bond, validatorId: Long): F[Long]
  def update(id: Long, bond: Bond, validatorId: Long): F[Unit]

  def getById(id: Long): F[Option[Bond]]
}
