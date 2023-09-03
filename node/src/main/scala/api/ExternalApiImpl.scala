package api

import cats.effect.Sync
import sdk.api.ExternalApi
import sdk.api.data.*
import sdk.db.DbSession.withSessionF
import sdk.db.{DbSession, DeployTable}
import squeryl.DbQuery

/**
 * Implementation of the external API using `squeryl` library.
 */
class ExternalApiImpl[F[_]: Sync: DbSession] extends ExternalApi[F] {
  override def getBlockById(blockId: Long): F[Option[Block]] = ???

  override def getDeploysByBlockId(blockId: Long): F[Seq[Deploy]] =
    withSessionF(DbQuery.getDeploysByBlockId(blockId).iterator.map(DeployTable.fromDb).toSeq)
}
