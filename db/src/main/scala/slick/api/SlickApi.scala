package slick.api

import cats.effect.Async
import cats.syntax.all.*
import sdk.comm.Peer
import sdk.primitive.ByteArray
import slick.syntax.all.*
import slick.{Actions, SlickDb}

import scala.concurrent.ExecutionContext

object SlickApi {
  def apply[F[_]: Async](db: SlickDb): F[SlickApi[F]] =
    Async[F].executionContext.map(ec => new SlickApi(db, ec))

  def apply[F[_]](implicit s: SlickApi[F]): SlickApi[F] = s
}

class SlickApi[F[_]: Async](db: SlickDb, ec: ExecutionContext) {
  implicit val slickDb: SlickDb = db

  val actions: Actions = Actions(db.profile, ec)

  def getConfig(key: String): F[Option[String]] = actions.getConfig(key).run

  def putConfig(key: String, value: String): F[Unit] = actions.putConfig(key, value).run.void

  def peers: F[Seq[Peer]] = actions.peers.run

  def deployInsert(d: sdk.data.Deploy): F[Unit] =
    actions
      .deployInsertIfNot(
        data.Deploy(d.sig.bytes, d.deployerPk.bytes, d.shardName, d.program, d.phloPrice, d.phloLimit, d.nonce),
      )
      .run
      .void

  def deployGet(sig: ByteArray): F[Option[sdk.data.Deploy]] = actions
    .deployGetData(sig.bytes)
    .run
    .map(
      _.map(d =>
        sdk.data.Deploy(
          sig = ByteArray(d.sig),
          deployerPk = ByteArray(d.deployerPk),
          shardName = d.shardName,
          program = d.program,
          phloPrice = d.phloPrice,
          phloLimit = d.phloLimit,
          nonce = d.nonce,
        ),
      ),
    )

  /** DeploySet */
  def deploySetInsert(deploySetHash: ByteArray, deploySigs: Set[ByteArray]): F[Unit] =
    actions.deploySetInsertIfNot(deploySetHash.bytes, deploySigs.toSeq.map(_.bytes)).run.void

  /** BlockSet */
  def blockSetInsert(blockSetHash: ByteArray, blockHashes: Set[ByteArray]): F[Unit] =
    actions.blockSetInsertIfNot(blockSetHash.bytes, blockHashes.toSeq.map(_.bytes)).run.void

  /** BondsMap */
  def bondsMapInsert(bondsMapHash: ByteArray, bMap: Map[ByteArray, Long]): F[Unit] =
    actions.bondsMapInsertIfNot(bondsMapHash.bytes, bMap.toSeq.map(x => (x._1.bytes, x._2))).run.void

  def blockInsert(b: sdk.data.Block)(
    justificationSetHash: ByteArray,
    offencesSetHash: ByteArray,
    bondsMapHash: ByteArray,
    finalFringeHash: ByteArray,
    execDeploySetHash: ByteArray,
    mergeDeploySetHash: ByteArray,
    dropDeploySetHash: ByteArray,
    mergeDeploySetFinalHash: ByteArray,
    dropDeploySetFinalHash: ByteArray,
  ): F[Unit] = actions
    .blockInsertIfNot(
      data.Block(
        version = b.version,
        hash = b.hash.bytes,
        sigAlg = b.sigAlg,
        signature = b.signature.bytes,
        finalStateHash = b.finalStateHash.bytes,
        postStateHash = b.postStateHash.bytes,
        validatorPk = b.validatorPk.bytes,
        shardName = b.shardName,
        justificationSet = data.SetData(justificationSetHash.bytes, b.justificationSet.toSeq.map(_.bytes)),
        seqNum = b.seqNum,
        offencesSet = data.SetData(offencesSetHash.bytes, b.offencesSet.toSeq.map(_.bytes)),
        bondsMap = data.BondsMapData(bondsMapHash.bytes, b.bondsMap.toSeq.map(x => (x._1.bytes, x._2))),
        finalFringe = data.SetData(finalFringeHash.bytes, b.finalFringe.toSeq.map(_.bytes)),
        execDeploySet = data.SetData(execDeploySetHash.bytes, b.execDeploySet.toSeq.map(_.bytes)),
        mergeDeploySet = data.SetData(mergeDeploySetHash.bytes, b.mergeDeploySet.toSeq.map(_.bytes)),
        dropDeploySet = data.SetData(dropDeploySetHash.bytes, b.dropDeploySet.toSeq.map(_.bytes)),
        mergeDeploySetFinal = data.SetData(mergeDeploySetFinalHash.bytes, b.mergeDeploySetFinal.toSeq.map(_.bytes)),
        dropDeploySetFinal = data.SetData(dropDeploySetFinalHash.bytes, b.dropDeploySetFinal.toSeq.map(_.bytes)),
      ),
    )
    .run
    .void

  def blockGet(hash: ByteArray): F[Option[sdk.data.Block]] = actions
    .blockGetData(hash.bytes)
    .run
    .map(
      _.map(b =>
        sdk.data.Block(
          version = b.version,
          hash = ByteArray(b.hash),
          sigAlg = b.sigAlg,
          signature = ByteArray(b.signature),
          finalStateHash = ByteArray(b.finalStateHash),
          postStateHash = ByteArray(b.postStateHash),
          validatorPk = ByteArray(b.validatorPk),
          shardName = b.shardName,
          justificationSet = b.justificationSet.data.map(ByteArray(_)).toSet,
          seqNum = b.seqNum,
          offencesSet = b.offencesSet.data.map(ByteArray(_)).toSet,
          bondsMap = b.bondsMap.data.map(x => (ByteArray(x._1), x._2)).toMap,
          finalFringe = b.finalFringe.data.map(ByteArray(_)).toSet,
          execDeploySet = b.execDeploySet.data.map(ByteArray(_)).toSet,
          mergeDeploySet = b.mergeDeploySet.data.map(ByteArray(_)).toSet,
          dropDeploySet = b.dropDeploySet.data.map(ByteArray(_)).toSet,
          mergeDeploySetFinal = b.mergeDeploySetFinal.data.map(ByteArray(_)).toSet,
          dropDeploySetFinal = b.dropDeploySetFinal.data.map(ByteArray(_)).toSet,
        ),
      ),
    )

  def isBlockExist(hash: ByteArray): F[Boolean] = actions.isBlockExist(hash).run
}
