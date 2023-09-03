package squeryl.api

import cats.data.OptionT
import cats.effect.Sync
import cats.syntax.all.*
import sdk.api.BlockDbApi
import sdk.api.data.*
import squeryl.RhonixNodeDb.blockTable
import squeryl.tables.BlockTable
import squeryl.tables.CustomTypeMode.*
import squeryl.{withSession, SqlConn}

final case class BlockDbApiImpl[F[_]: Sync: SqlConn](
  validatorDbApi: ValidatorDbApiImpl[F],
  blockJustificationsDbApi: BlockJustificationsDbApiImpl[F],
  blockBondsDbApi: BlockBondsDbApiImpl[F],
  blockDeploysDbApi: BlockDeploysDbApiImpl[F],
  bondDbApi: BondDbApiImpl[F],
  deployDbApi: DeployDbApiImpl[F],
) extends BlockDbApi[F] {
  override def insert(block: Block, senderId: Long): F[Long] =
    withSession(blockTable.insert(BlockTable.toDb(0L, block, senderId))).map(_.id)

  override def update(id: Long, block: Block, senderId: Long): F[Unit] =
    withSession(blockTable.update(BlockTable.toDb(id, block, senderId)))

  override def getById(id: Long): F[Option[Block]] = (for {
    block  <- OptionT(withSession(blockTable.where(_.id === id).headOption))
    sender <- OptionT(validatorDbApi.getById(block.senderId))

    justifications <- OptionT.liftF(justifications(block.id))
    bonds          <- OptionT.liftF(bonds(block.id))
    deploys        <- OptionT.liftF(deploys(block.id))

  } yield BlockTable.fromDb(block, sender, justifications, bonds, deploys)).value

  override def getByHash(hash: Array[Byte]): F[Option[Block]] =
    (for {
      blockByHash <- OptionT(withSession(blockTable.where(_.hash === hash).headOption))
      blockById   <- OptionT.liftF(getById(blockByHash.id)).flattenOption
    } yield blockById).value

  // The functions below can be implemented at the DBMS level using join as shown in the example
  // withSession {
  //      join(validatorTable, blockJustificationsTable)((validator, blockJustifications) =>
  //        where(blockJustifications.latestBlockId === blockId)
  //        select validator
  //          on(blockJustifications.validatorId === validator.id),
  //      ).toSet.map(Validator.fromDb)
  //    }
  private def justifications(blockId: Long): F[Set[Validator]] = for {
    blockJustifications <- blockJustificationsDbApi.getByBlock(blockId)
    validators          <- blockJustifications.traverse(bj => validatorDbApi.getById(bj.validatorId))
  } yield validators.flatten.toSet

  private def bonds(blockId: Long): F[Set[Bond]] = for {
    blockBonds <- blockBondsDbApi.getByBlock(blockId)
    bonds      <- blockBonds.traverse(bb => bondDbApi.getById(bb.bondId))
  } yield bonds.flatten.toSet

  private def deploys(blockId: Long): F[Set[Deploy]] = for {
    blockDeploys <- blockDeploysDbApi.getByBlock(blockId)
    deploys      <- blockDeploys.traverse(bd => deployDbApi.getById(bd.deployId))
  } yield deploys.flatten.toSet
}
