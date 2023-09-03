package squeryl.tables

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
