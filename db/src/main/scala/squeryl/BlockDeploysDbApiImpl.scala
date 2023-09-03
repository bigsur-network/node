package squeryl

import cats.effect.Sync
import cats.syntax.all.*
import sdk.CustomTypeMode.*
import sdk.RhonixNodeDb.blockDeploysTable
import sdk.api.data.*
import sdk.db.DbSession.withSessionF
import sdk.db.{BlockDeploysDbApi, BlockDeploysTable, DbSession}

class BlockDeploysDbApiImpl[F[_]: Sync: DbSession] extends BlockDeploysDbApi[F] {
  override def insert(blockDeploys: BlockDeploys): F[BlockDeploys] =
    withSessionF(blockDeploysTable.insert(BlockDeploysTable.toDb(blockDeploys))).map(BlockDeploysTable.fromDb)

  override def getByBlock(blockId: Long): F[Seq[BlockDeploys]] = withSessionF(
    blockDeploysTable.where(_.blockId === blockId).map(BlockDeploysTable.fromDb).toSeq,
  )
}

object BlockDeploysDbApiImpl {
  def apply[F[_]: BlockDeploysDbApiImpl]: BlockDeploysDbApiImpl[F] = implicitly[BlockDeploysDbApiImpl[F]]
}
