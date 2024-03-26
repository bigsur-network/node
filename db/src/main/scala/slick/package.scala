import slick.lifted.TableQuery
import slick.tables.*

package object slick {
  // all queries
  val qBlocks         = TableQuery[TableBlocks]
  val qBlockSets      = TableQuery[TableBlockSets]
  val qBlockSetBinds  = TableQuery[TableBlockSetBinds]
  val qBonds          = TableQuery[TableBonds]
  val qBondsMaps      = TableQuery[TableBondsMaps]
  val qDeploys        = TableQuery[TableDeploys]
  val qDeployers      = TableQuery[TableDeployers]
  val qDeploySets     = TableQuery[TableDeploySets]
  val qDeploySetBinds = TableQuery[TableDeploySetBinds]
  val qShards         = TableQuery[TableShards]
  val qValidators     = TableQuery[TableValidators]
  val qConfigs        = TableQuery[TableConfig]
  val qPeers          = TableQuery[TablePeers]
}
