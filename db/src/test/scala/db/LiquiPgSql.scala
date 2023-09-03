package db

import cats.effect.kernel.{Resource, Sync}
import io.zonky.test.db.postgres.embedded.LiquibasePreparer
import io.zonky.test.db.postgres.junit5.EmbeddedPostgresExtension
import org.junit.jupiter.api.extension.ExtensionContext
import org.mockito.Mockito.mock
import squeryl.SqlConn
import cats.syntax.all.*

/** Embedded PgSql initialised via liquibase. */
object LiquiPgSql {
  def apply[F[_]: Sync]: Resource[F, SqlConn[F]] = Resource.make {
    Sync[F].delay {
      val db = LiquibasePreparer.forClasspathLocation("liquibase/changelog.yaml")
      val x  = EmbeddedPostgresExtension.preparedDatabase(db)
      x.beforeEach(mock(classOf[ExtensionContext]))
      SqlConn(x.getTestDatabase.getConnection)
    }
  }(conn => conn.get.map(_.close())) // use Resource to ensure connection does not leak
}
