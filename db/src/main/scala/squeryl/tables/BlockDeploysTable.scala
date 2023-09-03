package squeryl.tables

import sdk.api.data.*

@SuppressWarnings(Array("org.wartremover.warts.FinalCaseClass"))
case class BlockDeploysTable(blockId: Long, deployId: Long)

object BlockDeploysTable {
  def toDb(blockDeploys: BlockDeploys): BlockDeploysTable =
    BlockDeploysTable(blockDeploys.blockId, blockDeploys.deployId)

  def fromDb(blockDeploys: BlockDeploysTable): BlockDeploys =
    BlockDeploys(blockDeploys.blockId, blockDeploys.deployId)
}
