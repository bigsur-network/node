package squeryl.api

import cats.effect.Sync
import cats.syntax.all.*
import sdk.api.BlockDeploysDbApi
import sdk.api.data.*
import sdk.db.SqlConn

import java.sql.Connection
import squeryl.withSession
import squeryl.RhonixNodeDb.blockDeploysTable
import squeryl.tables.BlockDeploysTable
import squeryl.CustomTypeMode.*

class BlockDeploysDbApiImpl[F[_]: Sync: SqlConn] extends BlockDeploysDbApi[F] {
  override def insert(blockDeploys: BlockDeploys): F[Unit] =
    withSession { val _ = blockDeploysTable.insert(BlockDeploysTable.toDb(blockDeploys)); () }

  override def getByBlock(blockId: Long): F[Seq[BlockDeploys]] = withSession(
    blockDeploysTable.where(_.blockId === blockId).map(BlockDeploysTable.fromDb).toSeq,
  )
}

object BlockDeploysDbApiImpl {
  def apply[F[_]: BlockDeploysDbApiImpl]: BlockDeploysDbApiImpl[F] = implicitly[BlockDeploysDbApiImpl[F]]
}
