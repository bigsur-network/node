import org.squeryl.adapters.PostgreSqlAdapter
import sdk.db.DbSession
import squeryl.tables.CustomTypeMode

import java.sql.Connection

package object squeryl {
  def session[F[_]](connection: Connection): DbSession[F] = new DbSession[F] {
    override def using[A](a: => A): A = CustomTypeMode.using(session)(a)

    private lazy val session = {
      val _ = Class.forName("org.postgresql.Driver")
      org.squeryl.Session.create(connection, new PostgreSqlAdapter)
    }
  }
}
