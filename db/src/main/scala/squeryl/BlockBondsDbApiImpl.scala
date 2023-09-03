package squeryl

import cats.effect.Sync
import cats.syntax.all.*
import sdk.api.BlockBondsDbApi
import sdk.api.data.BlockBonds
import sdk.db.DbSession
import sdk.db.DbSession.withSessionF
import squeryl.RhonixNodeDb.blockBondsTable
import squeryl.tables.CustomTypeMode.*
import squeryl.tables.BlockBondsTable

class BlockBondsDbApiImpl[F[_]: Sync: DbSession] extends BlockBondsDbApi[F] {
  override def insert(blockBonds: BlockBonds): F[BlockBonds] =
    withSessionF(blockBondsTable.insert(BlockBondsTable.toDb(blockBonds))).map(BlockBondsTable.fromDb)

  override def getByBlock(blockId: Long): F[Seq[BlockBonds]] =
    withSessionF(blockBondsTable.where(_.blockId === blockId).map(BlockBondsTable.fromDb).toSeq)
}

object BlockBondsDbApiImpl {
  def apply[F[_]: BlockBondsDbApiImpl]: BlockBondsDbApiImpl[F] = implicitly[BlockBondsDbApiImpl[F]]
}
