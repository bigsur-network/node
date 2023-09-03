package api

import cats.effect.Sync
import sdk.api.ExternalApi
import sdk.api.data.*
import squeryl.{withSession, DbQuery, SqlConn}
import squeryl.tables.DeployTable

/**
 * Implementation of the external API using `squeryl` library.
 */
class ExternalApiImpl[F[_]: Sync: SqlConn] extends ExternalApi[F] {
  override def getBlockById(blockId: Long): F[Option[Block]] = ???

  override def getDeploysByBlockId(blockId: Long): F[Seq[Deploy]] =
    withSession(DbQuery.getDeploysByBlockId(blockId).iterator.map(DeployTable.fromDb).toSeq)
}
