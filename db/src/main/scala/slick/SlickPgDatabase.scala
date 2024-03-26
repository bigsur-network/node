package slick

import cats.effect.{Async, Resource, Sync}
import db.PgDataSource
import slick.jdbc.JdbcBackend.Database

object SlickPgDatabase {
  def apply[F[_]: Sync](config: db.Config): Resource[F, Database] =
    Resource.make {
      Sync[F].delay(Database.forDataSource(PgDataSource(config), None))
    } { db =>
      Sync[F].delay(db.close())
    }
}
