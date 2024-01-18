package slick

import slick.jdbc.JdbcProfile

final case class Queries(profile: JdbcProfile) {
  import profile.api.*

  /** Config */

  val configValue = Compiled((key: Rep[String]) => qConfigs.filter(_.name === key).map(_.value))

  /** Shard */

  val shardNameById = Compiled((shardId: Rep[Long]) => qShards.filter(_.id === shardId).map(_.name))

  val shardIdByName = Compiled((name: Rep[String]) => qShards.filter(_.name === name).map(_.id))

  /** Deployer */

  val deployerIdByPK = Compiled((pK: Rep[Array[Byte]]) => qDeployers.filter(_.pubKey === pK).map(_.id))

  /** Deploy */

  val deploys = Compiled(qDeploys.map(_.sig))

  private val deployBySig = Compiled((sig: Rep[Array[Byte]]) => qDeploys.filter(_.sig === sig))

  val deployIdBySig = Compiled((sig: Rep[Array[Byte]]) => deployBySig.extract(sig).map(_.id))

  def deployIdsBySigs(sigs: Seq[Array[Byte]]) = Compiled(qDeploys.filter(_.sig inSet sigs).map(_.id))

  val deployWithDataBySig = Compiled((sig: Rep[Array[Byte]]) =>
    for {
      deploy   <- deployBySig.extract(sig)
      shard    <- qShards if deploy.shardId === shard.id
      deployer <- qDeployers if deploy.deployerId === deployer.id
    } yield (deploy, deployer.pubKey, shard.name),
  )

  val deploySetData = Compiled((deploySetId: Rep[Long]) =>
    for {
      ds  <- qDeploySets.filter(_.id === deploySetId)
      dsb <- qDeploySetBinds if dsb.deploySetId === ds.id
      d   <- qDeploys if d.id === dsb.deployId
    } yield (ds.hash, d.sig),
  )

  /** Block */

  val blocks = Compiled(qBlocks.map(_.hash))

  val blockIdByHash = Compiled((hash: Rep[Array[Byte]]) => blockByHash.extract(hash).map(_.id))

  val blockByHash = Compiled((hash: Rep[Array[Byte]]) => qBlocks.filter(_.hash === hash))

  /** DeploySet */

  val deploySetIdByHash = Compiled((hash: Rep[Array[Byte]]) => qDeploySets.filter(_.hash === hash).map(_.id))

  /** BlockSet */

  val blockSetIdByHash = Compiled((hash: Rep[Array[Byte]]) => qBlockSets.filter(_.hash === hash).map(_.id))

  val blockSetData = Compiled((blockSetId: Rep[Long]) =>
    for {
      bs  <- qBlockSets.filter(_.id === blockSetId)
      bsb <- qBlockSetBinds if bsb.blockSetId === bs.id
      b   <- qBlocks if b.id === bsb.blockId
    } yield (bs.hash, b.hash),
  )

  /** BondsMap */

  val bondsMap = Compiled(qBondsMaps.map(_.hash))

  val bondsMapIdByHash = Compiled((hash: Rep[Array[Byte]]) => qBondsMaps.filter(_.hash === hash).map(_.id))

  val bondsMapData = Compiled((bondsMapId: Rep[Long]) =>
    for {
      bm <- qBondsMaps.filter(_.id === bondsMapId)
      b  <- qBonds if b.bondsMapId === bm.id
      v  <- qValidators if v.id === b.validatorId
    } yield (bm.hash, (v.pubKey, b.stake)),
  )

  /** Validator */

  val validatorIdByPK = Compiled((pK: Rep[Array[Byte]]) => qValidators.filter(_.pubKey === pK).map(_.id))

  val validatorPkById = Compiled((validatorId: Rep[Long]) => qValidators.filter(_.id === validatorId).map(_.pubKey))
}