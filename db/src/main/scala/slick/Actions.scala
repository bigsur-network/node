package slick

import cats.syntax.all.*
import sdk.comm.Peer
import sdk.error.FatalError
import sdk.primitive.ByteArray
import sdk.syntax.all.*
import slick.api.data.{Block, BondsMapData, SetData}
import slick.dbio.Effect.*
import slick.jdbc.JdbcProfile
import slick.lifted.CompiledFunction
import slick.tables.*

import scala.concurrent.ExecutionContext

final case class Actions(profile: JdbcProfile, ec: ExecutionContext) {
  private val queries: Queries       = Queries(profile)
  import profile.api.*
  implicit val _ec: ExecutionContext = ec

  /** Config */

  def putConfig(key: String, value: String): DBIOAction[Int, NoStream, Write] =
    qConfigs.insertOrUpdate((key, value))

  def getConfig(key: String): DBIOAction[Option[String], NoStream, Read] =
    queries.configValue(key).result.headOption

  /** Shard */

  private def shardInsertIfNot(name: String): DBIOAction[Long, NoStream, All] = {
    def shardInsert(name: String): DBIOAction[Long, NoStream, Write] =
      (qShards.map(_.name) returning qShards.map(_.id)) += name

    insertIfNot(name, queries.shardIdByName, name, shardInsert)
  }

  /** Deployer */

  private def deployerInsertIfNot(pK: Array[Byte]): DBIOAction[Long, NoStream, All] = {
    def deployerInsert(pK: Array[Byte]): DBIOAction[Long, NoStream, Write] =
      (qDeployers.map(_.pubKey) returning qDeployers.map(_.id)) += pK

    insertIfNot(pK, queries.deployerIdByPK, pK, deployerInsert)
  }

  /** Deploy */

  /** Get deploy by unique sig. Returned (TableDeploys.Deploy, shard.name, deployer.pubKey) */
  def deployGetData(sig: Array[Byte]): DBIOAction[Option[api.data.Deploy], NoStream, Read] =
    queries
      .deployWithDataBySig(sig)
      .result
      .headOption
      .map(_.map { case (d, pK, sName) =>
        api.data.Deploy(
          sig = d.sig,
          deployerPk = pK,
          shardName = sName,
          program = d.program,
          phloPrice = d.phloPrice,
          phloLimit = d.phloLimit,
          nonce = d.nonce,
        )
      })

  /** Insert a new record in table if there is no such entry. Returned id */
  def deployInsertIfNot(d: api.data.Deploy): DBIOAction[Long, NoStream, All] = {

    /** Insert a new record in table. Returned id. */
    def deployInsert(deploy: TableDeploys.Deploy): DBIOAction[Long, NoStream, Write] =
      (queries.deploysCompiled returning qDeploys.map(_.id)) += deploy

    val actions = for {
      deployerId <- deployerInsertIfNot(d.deployerPk)
      shardId    <- shardInsertIfNot(d.shardName)
      newDeploy   = TableDeploys.Deploy(
                      id = 0L, // Will be replaced by AutoInc
                      sig = d.sig,
                      deployerId = deployerId,
                      shardId = shardId,
                      program = d.program,
                      phloPrice = d.phloPrice,
                      phloLimit = d.phloLimit,
                      nonce = d.nonce,
                    )
      deployId   <- insertIfNot(d.sig, queries.deployIdBySig, newDeploy, deployInsert)
    } yield deployId

    actions.transactionally
  }

  /** Block */

  /** Insert a new Block in table if there is no such entry. Returned id */
  def blockInsertIfNot(b: api.data.Block): DBIOAction[Long, NoStream, All] = {

    def insertBlockSet(setData: api.data.SetData): DBIOAction[Option[Long], NoStream, All] =
      blockSetInsertIfNot(setData.hash, setData.data).map(_.some)

    def insertDeploySet(setData: api.data.SetData): DBIOAction[Option[Long], NoStream, All] =
      deploySetInsertIfNot(setData.hash, setData.data).map(_.some)

    def blockInsert(block: TableBlocks.Block): DBIOAction[Long, NoStream, Write] =
      (queries.blocksCompiled returning qBlocks.map(_.id)) += block

    val actions = for {
      validatorId <- validatorInsertIfNot(b.validatorPk)
      shardId     <- shardInsertIfNot(b.shardName)

      justificationSetId <- insertBlockSet(b.justificationSet)
      offencesSet        <- insertBlockSet(b.offencesSet)
      bondsMapId         <- bondsMapInsertIfNot(b.bondsMap.hash, b.bondsMap.data)
      finalFringe        <- insertBlockSet(b.finalFringe)
      deploySetId        <- insertDeploySet(b.execDeploySet)

      mergeSetId      <- insertDeploySet(b.mergeDeploySet)
      dropSetId       <- insertDeploySet(b.dropDeploySet)
      mergeSetFinalId <- insertDeploySet(b.mergeDeploySetFinal)
      dropSetFinalId  <- insertDeploySet(b.dropDeploySetFinal)

      newBlock = TableBlocks.Block(
                   id = 0L,
                   version = b.version,
                   hash = b.hash,
                   sigAlg = b.sigAlg,
                   signature = b.signature,
                   finalStateHash = b.finalStateHash,
                   postStateHash = b.postStateHash,
                   validatorId = validatorId,
                   shardId = shardId,
                   justificationSetId = justificationSetId,
                   seqNum = b.seqNum,
                   offencesSetId = offencesSet,
                   bondsMapId = bondsMapId,
                   finalFringeId = finalFringe,
                   execDeploySetId = deploySetId,
                   mergeDeploySetId = mergeSetId,
                   dropDeploySetId = dropSetId,
                   mergeDeploySetFinalId = mergeSetFinalId,
                   dropDeploySetFinalId = dropSetFinalId,
                 )

      blockId <- insertIfNot(b.hash, queries.blockIdByHash, newBlock, blockInsert)
    } yield blockId

    actions.transactionally
  }

  /** Get deploy by unique sig. Returned (TableDeploys.Deploy, shard.name, deployer.pubKey) */
  def blockGetData(
    hash: Array[Byte],
  ): DBIOAction[Option[api.data.Block], NoStream, Read & Transactional] =
    queries
      .blockByHash(hash)
      .result
      .headOption
      .flatMap {
        case Some(block) => getBlockData(block).map(_.some)
        case None        => DBIO.successful(None)
      }
      .transactionally

  private def getBlockData(b: TableBlocks.Block): DBIOAction[Block, NoStream, Read] =
    (for {
      validatorPKOpt          <- queries.validatorPkById(b.validatorId).result.headOption
      shardNameOpt            <- queries.shardNameById(b.shardId).result.headOption
      justificationSetDataOpt <- getBlockSetData(b.justificationSetId)
      offencesSetDataOpt      <- getBlockSetData(b.offencesSetId)
      bondsMapDataOpt         <- getBondsMapData(b.bondsMapId)
      finalFringeDataOpt      <- getBlockSetData(b.finalFringeId)
      deploySetDataOpt        <- getDeploySetData(b.execDeploySetId)
      mergeSetDataOpt         <- getDeploySetData(b.mergeDeploySetId)
      dropSetDataOpt          <- getDeploySetData(b.dropDeploySetId)
      mergeSetFinalDataOpt    <- getDeploySetData(b.mergeDeploySetFinalId)
      dropSetFinalDataOpt     <- getDeploySetData(b.dropDeploySetFinalId)
    } yield for {
      validatorPK          <- validatorPKOpt
      shardName            <- shardNameOpt
      justificationSetData <- justificationSetDataOpt
      offencesSetData      <- offencesSetDataOpt
      bondsMapData         <- bondsMapDataOpt
      finalFringeData      <- finalFringeDataOpt
      deploySetData        <- deploySetDataOpt
      mergeSetData         <- mergeSetDataOpt
      dropSetData          <- dropSetDataOpt
      mergeSetFinalData    <- mergeSetFinalDataOpt
      dropSetFinalData     <- dropSetFinalDataOpt
    } yield api.data.Block(
      version = b.version,
      hash = b.hash,
      sigAlg = b.sigAlg,
      signature = b.signature,
      finalStateHash = b.finalStateHash,
      postStateHash = b.postStateHash,
      validatorPk = validatorPK,
      shardName = shardName,
      justificationSet = justificationSetData,
      seqNum = b.seqNum,
      offencesSet = offencesSetData,
      bondsMap = bondsMapData,
      finalFringe = finalFringeData,
      execDeploySet = deploySetData,
      mergeDeploySet = mergeSetData,
      dropDeploySet = dropSetData,
      mergeDeploySetFinal = mergeSetFinalData,
      dropDeploySetFinal = dropSetFinalData,
    )).flatMap {
      case Some(block) => DBIO.successful(block)
      case None        => DBIO.failed(FatalError(s"Block data for hash ${ByteArray(b.hash).toHex} is incomplete"))
    }

  private def getBlockSetData(idOpt: Option[Long]): DBIOAction[Option[SetData], NoStream, Read] =
    idOpt
      .map { id =>
        for {
          blockSetDataById <- queries.blockSetData(id).result
          blockSetHashOpt  <- queries.blockSetHashById(id).result.headOption
        } yield {
          val blockSetDataOpt = blockSetDataById
            .groupBy { case (bsHash, _) => ByteArray(bsHash) }
            .headOption
            .map { case (bsHash, group) =>
              api.data.SetData(bsHash.bytes, group.map { case (_, bHash) => bHash })
            }

          val emptySetDataOpt = blockSetHashOpt.map(hash => api.data.SetData(hash, Seq.empty[Array[Byte]]))
          blockSetDataOpt.orElse(emptySetDataOpt)
        }
      }
      .getOrElse(DBIO.successful(None))

  private def getDeploySetData(idOpt: Option[Long]): DBIOAction[Option[SetData], NoStream, Read] =
    idOpt
      .map { id =>
        for {
          deploySetDataById <- queries.deploySetData(id).result
          deploySetHashOpt  <- queries.deploySetHashById(id).result.headOption
        } yield {
          val deploySetDataOpt = deploySetDataById
            .groupBy { case (dsHash, _) => ByteArray(dsHash) }
            .headOption
            .map { case (dsHash, group) =>
              api.data.SetData(dsHash.bytes, group.map { case (_, dSig) => dSig })
            }
          val emptySetDataOpt  = deploySetHashOpt.map(hash => api.data.SetData(hash, Seq.empty[Array[Byte]]))
          deploySetDataOpt.orElse(emptySetDataOpt)
        }
      }
      .getOrElse(DBIO.successful(None))

  private def getBondsMapData(id: Long): DBIOAction[Option[BondsMapData], NoStream, Read] =
    queries.bondsMapData(id).result.map(_.groupBy { case (bmHash, _) => ByteArray(bmHash) }.headOption).map {
      _.map { case (bmHash, data) =>
        BondsMapData(bmHash.bytes, data.map { case (_, (pubKey, stake)) => (pubKey, stake) })
      }
    }

  def isBlockExist(hash: ByteArray): DBIOAction[Boolean, NoStream, Read] =
    queries.blockIdByHash(hash.bytes).result.headOption.map(_.isDefined)

  /** DeploySet */

  /** Insert a new deploy set in table if there is no such entry. Returned id */
  def deploySetInsertIfNot(hash: Array[Byte], deploySigs: Seq[Array[Byte]]): DBIOAction[Long, NoStream, All] = {

    def deploySetInsert(hash: Array[Byte]): DBIOAction[Long, NoStream, Write] =
      (queries.deploySetsCompiled returning qDeploySets.map(_.id)) += hash

    def insertBinds(deploySetId: Long, deployIds: Seq[Long]): DBIOAction[Option[Int], NoStream, Write] =
      queries.deploySetBindsCompiled ++= deployIds.map(TableDeploySetBinds.DeploySetBind(deploySetId, _))

    def deployIdsBySigs(sigs: Seq[Array[Byte]]): DBIOAction[Seq[Long], NoStream, Read] =
      DBIO.sequence(sigs.map(queries.deployIdBySig(_).result.headOption)).map(_.flatten)

    def insertAllData(in: (Array[Byte], Seq[Array[Byte]])): DBIOAction[Long, NoStream, Write & Read & Transactional] =
      in match {
        case (hash, deploySigs) =>
          (for {
            deploySetId <- deploySetInsert(hash)
            deployIds   <- deployIdsBySigs(deploySigs)
            _           <- if (deployIds.length == deploySigs.length) insertBinds(deploySetId, deployIds)
                           else
                             DBIO.failed(
                               new RuntimeException(
                                 "Signatures of deploys added to deploy set do not match deploys in deploy table",
                               ),
                             )
          } yield deploySetId).transactionally
      }

    insertIfNot(hash, queries.deploySetIdByHash, (hash, deploySigs), insertAllData)
  }

  /** BlockSet */

  /** Insert a new block set in table if there is no such entry. Returned id */
  def blockSetInsertIfNot(hash: Array[Byte], blockHashes: Seq[Array[Byte]]): DBIOAction[Long, NoStream, All] = {
    def blockSetInsert(hash: Array[Byte]): DBIOAction[Long, NoStream, Write] =
      (queries.blockSetsCompiled returning qBlockSets.map(_.id)) += hash

    def getBlockIdsByHashes(hashes: Seq[Array[Byte]]): DBIOAction[Seq[Long], NoStream, Read] =
      DBIO.sequence(hashes.map(queries.blockIdByHash(_).result.headOption)).map(_.flatten)

    def insertBinds(blockSetId: Long, blockIds: Seq[Long]): DBIOAction[Option[Int], NoStream, Write] = {
      val binds = blockIds.map(TableBlockSetBinds.BlockSetBind(blockSetId, _))
      queries.blockSetBindsCompiled ++= binds
    }

    def insertAllData(in: (Array[Byte], Seq[Array[Byte]])): DBIOAction[Long, NoStream, Write & Read] = in match {
      case (hash, blockHashes) =>
        for {
          blockSetId <- blockSetInsert(hash)
          blockIds   <- getBlockIdsByHashes(blockHashes)
          _          <- if (blockIds.length == blockHashes.length) insertBinds(blockSetId, blockIds)
                        else
                          DBIO.failed(
                            new RuntimeException("Hashes of blocks added to block set do not match blocks in block table"),
                          )
        } yield blockSetId
    }

    insertIfNot(hash, queries.blockSetIdByHash, (hash, blockHashes), insertAllData)
  }

  /** BondsMap */

  /** Insert a new bonds map in table if there is no such entry. Returned id */
  def bondsMapInsertIfNot(hash: Array[Byte], bMap: Seq[(Array[Byte], Long)]): DBIOAction[Long, NoStream, All] = {

    def bondsMapInsert(hash: Array[Byte]): DBIOAction[Long, NoStream, Write] =
      (qBondsMaps.map(_.hash) returning qBondsMaps.map(_.id)) += hash

    def bondsInsert(bondsMapId: Long, bMap: Seq[(Long, Long)]): DBIOAction[Option[Int], NoStream, Write] = {
      val bonds = bMap.map { case (validatorId, stake) =>
        TableBonds.Bond(bondsMapId, validatorId, stake)
      }
      qBonds ++= bonds
    }

    def insertAllData(
      in: (Array[Byte], Seq[(Array[Byte], Long)]),
    ): DBIOAction[Long, NoStream, All] =
      in match {
        case (hash, bMapSeq) =>
          for {
            bondsMapId            <- bondsMapInsert(hash)
            validatorsIdWithStake <- DBIO.sequence(bMapSeq.map { case (validatorPk, stake) =>
                                       validatorInsertIfNot(validatorPk).map(validatorId => (validatorId, stake))
                                     })
            _                     <- bondsInsert(bondsMapId, validatorsIdWithStake)
          } yield bondsMapId
      }

    insertIfNot(hash, queries.bondsMapIdByHash, (hash, bMap), insertAllData)
  }

  /** Validator */

  private def validatorInsertIfNot(pK: Array[Byte]): DBIOAction[Long, NoStream, All] = {
    def validatorInsert(pK: Array[Byte]): DBIOAction[Long, NoStream, Write] =
      (qValidators.map(_.pubKey) returning qValidators.map(_.id)) += pK

    insertIfNot(pK, queries.validatorIdByPK, pK, validatorInsert)
  }

  /** Peer */

  def peers: DBIOAction[Seq[Peer], NoStream, Read] =
    queries.peersCompiled.result.map(_.map(peer => Peer(peer.host, peer.port, peer.isSelf, peer.isValidator)))

  def peerInsertIfNot(
    host: String,
    port: Int,
    isSelf: Boolean,
    isValidator: Boolean,
  ): DBIOAction[Long, profile.api.NoStream, All] = {
    def peerInsert(peer: TablePeers.Peer): DBIOAction[Long, NoStream, All] =
      (queries.peersCompiled returning qPeers.map(_.id)) += peer

    insertIfNot(host, queries.peerIdByPk, TablePeers.Peer(0L, host, port, isSelf, isValidator), peerInsert)
  }

  def removePeer(url: String): DBIOAction[Int, NoStream, Write] = queries.peerIdByPk(url).delete

  /** Insert a new record in table if there is no such entry. Returned id */
  private def insertIfNot[A, B](
    unique: A,
    getIdByUnique: CompiledFunction[
      Rep[A] => Query[Rep[Long], Long, Seq],
      Rep[A],
      A,
      Query[Rep[Long], Long, Seq],
      Seq[Long],
    ],
    insertable: B,
    insert: B => DBIOAction[Long, NoStream, All],
  ): DBIOAction[Long, NoStream, All] =
    getIdByUnique(unique).result.headOption
      .flatMap(_.map(DBIO.successful).getOrElse(insert(insertable)))
      .transactionally
}
