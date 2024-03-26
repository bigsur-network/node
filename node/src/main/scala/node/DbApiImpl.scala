package node

import cats.Eval
import cats.data.OptionT
import cats.effect.Sync
import cats.syntax.all.*
import node.DbApiImpl.*
import node.Hashing.*
import sdk.serialize.auto.*
import sdk.codecs.Serialize
import sdk.data.{BalancesDeploy, BalancesDeployBody, BalancesState}
import sdk.primitive.ByteArray
import sdk.syntax.all.digestSyntax
import slick.api.*

// TODO move somewhere
final case class DbApiImpl[F[_]: Sync](sApi: SlickApi[F]) {

  def saveBlock(b: dproc.data.Block.WithId[ByteArray, ByteArray, BalancesDeploy]): F[Unit] =
    for {
      _ <- b.m.txs.traverse(saveBalancesDeploy)
      _ <- b.m.merge.toSeq.traverse(saveBalancesDeploy)

      acceptedBalanceDeploys = if (b.m.finalized.isEmpty) Set() else b.m.finalized.get.accepted
      rejectedBalanceDeploys = if (b.m.finalized.isEmpty) Set() else b.m.finalized.get.rejected

      _ <- acceptedBalanceDeploys.toSeq.traverse(saveBalancesDeploy)
      _ <- rejectedBalanceDeploys.toSeq.traverse(saveBalancesDeploy)

      block = sdk.data.Block(
                version = ProtocolVersion,
                hash = b.id,
                sigAlg = SigAlg,
                signature = SignatureDefault,
                finalStateHash = b.m.finalStateHash,
                postStateHash = b.m.postStateHash,
                validatorPk = b.m.sender,
                shardName = ShardNameDefault,
                justificationSet = b.m.minGenJs,
                seqNum = SeqNumDefault,
                offencesSet = b.m.offences,
                bondsMap = b.m.bonds.bonds,
                finalFringe = b.m.finalFringe,
                execDeploySet = b.m.txs.map(_.id).toSet,
                mergeDeploySet = b.m.merge.map(_.id),
                dropDeploySet = Set(),
                mergeDeploySetFinal = acceptedBalanceDeploys.map(_.id),
                dropDeploySetFinal = rejectedBalanceDeploys.map(_.id),
              )

      _ <- sApi.blockInsert(block)(
             justificationSetHash = block.justificationSet.digest,
             offencesSetHash = calcSetHash(block.offencesSet),
             bondsMapHash = b.m.bonds.bonds.digest,
             finalFringeHash = calcSetHash(block.finalFringe),
             execDeploySetHash = calcSetHash(block.execDeploySet),
             mergeDeploySetHash = calcSetHash(block.mergeDeploySet),
             dropDeploySetHash = ByteArray.Default,
             mergeDeploySetFinalHash = calcSetHash(block.mergeDeploySetFinal),
             dropDeploySetFinalHash = calcSetHash(block.dropDeploySetFinal),
           )
    } yield ()

  def readBlock(id: ByteArray): F[Option[dproc.data.Block[ByteArray, ByteArray, BalancesDeploy]]] = {

    def decode(b: sdk.data.Block) =
      for {
        txsOpt      <- b.execDeploySet.toSeq.traverse(readBalancesDeploy)
        mergeOpt    <- b.mergeDeploySet.toSeq.traverse(readBalancesDeploy)
        acceptedOpt <- b.mergeDeploySetFinal.toSeq.traverse(readBalancesDeploy)
        rejectedOpt <- b.dropDeploySetFinal.toSeq.traverse(readBalancesDeploy)

        txs      <- txsOpt.traverse(safelyGet(_, "Missing exec deploy"))
        merge    <- mergeOpt.traverse(safelyGet(_, "Missing merge deploy"))
        accepted <- acceptedOpt.traverse(safelyGet(_, "Missing merge deploy final"))
        rejected <- rejectedOpt.traverse(safelyGet(_, "Missing drop deploy final"))

      } yield dproc.data.Block(
        sender = b.validatorPk,
        minGenJs = b.justificationSet,
        offences = b.offencesSet,
        txs = txs.toList,
        finalFringe = b.finalFringe,
        finalized =
          if (accepted.isEmpty && rejected.isEmpty) none
          else weaver.data.ConflictResolution[BalancesDeploy](accepted.toSet, rejected.toSet).some,
        merge = merge.toSet,
        bonds = weaver.data.Bonds(b.bondsMap),
        lazTol = LazTolDefault,
        expThresh = ExpThreshDefault,
        finalStateHash = b.finalStateHash,
        postStateHash = b.postStateHash,
      )
    OptionT(sApi.blockGet(id)).semiflatMap(decode).value
  }

  def isBlockExist(id: ByteArray): F[Boolean] = sApi.isBlockExist(id)

  def saveBalancesDeploy(d: BalancesDeploy): F[Unit] = sApi.deployInsert(
    sdk.data.Deploy(
      sig = d.id,
      deployerPk = DeployerPkDefault,
      shardName = ShardNameDefault,
      program = mapToString(d.body.state.diffs),
      phloPrice = PhloPriceDefault,
      phloLimit = PhloLimitDefault,
      nonce = d.body.vabn,
    ),
  )

  def readBalancesDeploy(id: ByteArray): F[Option[BalancesDeploy]] =
    OptionT(sApi.deployGet(id))
      .semiflatMap(d =>
        safelyGet(stringToMap(d.program), "Error during parsing of BalancesState").map(diffs =>
          BalancesDeploy(d.sig, BalancesDeployBody(BalancesState(diffs), d.nonce)),
        ),
      )
      .value
}

object DbApiImpl {

  private val ProtocolVersion  = 0
  private val SigAlg           = ""
  private val SignatureDefault = ByteArray.Default
  private val LazTolDefault    = 1
  private val ExpThreshDefault = 10000
  private val ShardNameDefault = "root"
  private val SeqNumDefault    = 0L

  private val DeployerPkDefault = ByteArray.Default
  private val PhloPriceDefault  = 0L
  private val PhloLimitDefault  = 0L

  /** Sort and concatenate elements of the set. Return hash of resulting sequence. */
  private def calcSetHash(set: Set[ByteArray]): ByteArray = {
    import sdk.hashing.ProtoBlakeHashing.SeqArrayCombinators
    ByteArray(set.toSeq.map(_.bytes).sortAndHash)
  }

  private def safelyGet[F[_]: Sync, A](opt: Option[A], errorMessage: String): F[A] =
    OptionT.fromOption[F](opt).getOrElseF(Sync[F].raiseError(new RuntimeException(errorMessage)))

  /** JSON encoding to a String */
  private def mapToString(map: Map[ByteArray, Long]): String = {
    import io.circe.syntax.*
    import io.circe.{Encoder, KeyEncoder}
    import sdk.codecs.*

    implicit val byteArrayEncoder: KeyEncoder[ByteArray] =
      KeyEncoder.encodeKeyString.contramap(x => Base16.encode(x.bytes))
    map.asJson(Encoder.encodeMap[ByteArray, Long]).noSpaces
  }

  /** JSON parsing from a String. Return None if parsing fails. */
  private def stringToMap(str: String): Option[Map[ByteArray, Long]] = {
    import io.circe.KeyDecoder
    import io.circe.parser.*
    import sdk.codecs.*

    implicit val byteArrayKeyDecoder: KeyDecoder[ByteArray] =
      KeyDecoder.instance(str => Base16.decode(str).toOption.map(ByteArray(_)))
    decode[Map[ByteArray, Long]](str).toOption
  }
}
