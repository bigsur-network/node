package slick.migrations

import slick.migration.api.{Dialect, ReversibleMigrationSeq, TableMigration}
import slick.*

// Initial version of the database
object Baseline {
  def apply(implicit dialect: Dialect[?]): ReversibleMigrationSeq = {
    val BlockTM = TableMigration(qBlocks).create
      .addColumns(
        _.id,
        _.version,
        _.hash,
        _.sigAlg,
        _.signature,
        _.finalStateHash,
        _.postStateHash,
        _.validatorId,
        _.shardId,
        _.justificationSetId,
        _.seqNum,
        _.offencesSetId,
        _.bondsMapId,
        _.finalFringeId,
        _.execDeploySetId,
        _.mergeDeploySetId,
        _.dropDeploySetId,
        _.mergeDeploySetFinalId,
        _.dropDeploySetFinalId,
      )
      .addForeignKeys(_.fk1, _.fk2, _.fk3, _.fk4, _.fk5, _.fk6, _.fk7, _.fk8, _.fk9, _.fk10, _.fk11)
      .addIndexes(_.idx)

    val BlockSetTM = TableMigration(qBlockSets).create
      .addColumns(_.id, _.hash)
      .addIndexes(_.idx)

    val bondTM = TableMigration(qBonds).create
      .addColumns(_.bondsMapId, _.validatorId, _.stake)
      .addPrimaryKeys(_.pk)
      .addForeignKeys(_.fk1, _.fk2)

    val BlockSetBindTM = TableMigration(slick.qBlockSetBinds).create
      .addColumns(_.blockSetId, _.blockId)
      .addPrimaryKeys(_.pk)
      .addForeignKeys(_.fk1, _.fk2)
      .addIndexes(_.idx)

    val bondsMapTM = TableMigration(qBondsMaps).create
      .addColumns(_.id, _.hash)
      .addIndexes(_.idx)

    val DeployTM = TableMigration(slick.qDeploys).create
      .addColumns(_.id, _.sig, _.deployerId, _.shardId, _.program, _.phloPrice, _.phloLimit, _.nonce)
      .addForeignKeys(_.fk1, _.fk2)
      .addIndexes(_.idxSig)
      .addIndexes(_.idxDeployerId)
      .addIndexes(_.idxShardId)

    val DeployerTM = TableMigration(slick.qDeployers).create
      .addColumns(_.id, _.pubKey)
      .addIndexes(_.idx)

    val DeploySetTM = TableMigration(slick.qDeploySets).create
      .addColumns(_.id, _.hash)
      .addIndexes(_.idx)

    val DeploySetBindTM = TableMigration(slick.qDeploySetBinds).create
      .addColumns(_.deploySetId, _.deployId)
      .addPrimaryKeys(_.pk)
      .addForeignKeys(_.fk1, _.fk2)
      .addIndexes(_.idx)

    val ShardTM = TableMigration(qShards).create
      .addColumns(_.id, _.name)
      .addIndexes(_.idx)

    val validatorTM = TableMigration(qValidators).create
      .addColumns(_.id, _.pubKey)
      .addIndexes(_.idx)

    val configTable = TableMigration(slick.qConfigs).create
      .addColumns(_.name, _.value)

    validatorTM & ShardTM & DeployerTM & DeployTM & DeploySetTM & DeploySetBindTM & bondsMapTM &
      bondTM & BlockSetTM & BlockTM & BlockSetBindTM & configTable
  }
}
