package slick

import slick.migration.api.{Dialect, MigrationSeq}

import scala.collection.SortedMap

/// Package for recording database migrations.
package object migrations {

  // This should be appended only.
  def all(dialect: Dialect[?]): SortedMap[Int, MigrationSeq] =
    SortedMap(
      1 -> Baseline(dialect),
      2 -> TablePeers(dialect),
    )
}
