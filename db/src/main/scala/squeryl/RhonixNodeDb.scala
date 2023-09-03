package squeryl

import org.squeryl.{Schema, Table}
import squeryl.tables.CustomTypeMode.*
import squeryl.tables.*

object RhonixNodeDb extends Schema {
  val validatorTable: Table[ValidatorTable]                     = table[ValidatorTable]("Validator")
  val bondTable: Table[BondTable]                               = table[BondTable]("Bond")
  val deployTable: Table[DeployTable]                           = table[DeployTable]("Deploy")
  val blockTable: Table[BlockTable]                             = table[BlockTable]("Block")
  val blockJustificationsTable: Table[BlockJustificationsTable] = table[BlockJustificationsTable]("BlockJustifications")
  val blockDeploysTable: Table[BlockDeploysTable]               = table[BlockDeploysTable]("BlockDeploys")
  val blockBondsTable: Table[BlockBondsTable]                   = table[BlockBondsTable]("BlockBonds")

  autoincrement(validatorTable)
  autoincrement(bondTable)
  autoincrement(deployTable)
  autoincrement(blockTable)

  private def autoincrement[A <: DbTable](table: Table[A]): Unit =
    on(table)(t => declare(t.id is autoIncremented(s"${t.name}_id_seq")))
}
