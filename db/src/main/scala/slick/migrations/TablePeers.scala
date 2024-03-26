package slick.migrations

import slick.migration.api.{Dialect, ReversibleMigrationSeq, TableMigration}
import slick.qPeers

object TablePeers {
  def apply(implicit dialect: Dialect[?]): ReversibleMigrationSeq =
    new ReversibleMigrationSeq(
      TableMigration(qPeers).create
        .addColumns(_.id, _.host, _.port, _.isSelf, _.isValidator)
        .addIndexes(_.idx),
    )
}
