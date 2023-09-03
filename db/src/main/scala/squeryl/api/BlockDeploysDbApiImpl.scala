package squeryl.api

import cats.effect.Sync
import cats.syntax.all.*
import sdk.api.BlockDeploysDbApi
import sdk.api.data.*

import java.sql.Connection
import squeryl.{withSession, SqlConn}
import squeryl.RhonixNodeDb.blockDeploysTable
import squeryl.tables.BlockDeploysTable
import squeryl.tables.CustomTypeMode.*

class BlockDeploysDbApiImpl[F[_]: Sync: SqlConn] extends BlockDeploysDbApi[F] {
  override def insert(blockDeploys: BlockDeploys): F[BlockDeploys] =
    withSession(blockDeploysTable.insert(BlockDeploysTable.toDb(blockDeploys))).map(BlockDeploysTable.fromDb)

  override def getByBlock(blockId: Long): F[Seq[BlockDeploys]] = withSession(
    blockDeploysTable.where(_.blockId === blockId).map(BlockDeploysTable.fromDb).toSeq,
  )
}

object BlockDeploysDbApiImpl {
  def apply[F[_]: BlockDeploysDbApiImpl]: BlockDeploysDbApiImpl[F] = implicitly[BlockDeploysDbApiImpl[F]]
}
