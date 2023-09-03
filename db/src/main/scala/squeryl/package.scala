import cats.Applicative
import org.squeryl.adapters.PostgreSqlAdapter
import squeryl.tables.CustomTypeMode
import cats.syntax.all.*

package object squeryl {
  def withSession[F[_]: Applicative: SqlConn, A](f: => A): F[A] = SqlConn[F].get.map { connection =>
    val _       = Class.forName("org.postgresql.Driver")
    val session = org.squeryl.Session.create(connection, new PostgreSqlAdapter)
    CustomTypeMode.using(session)(f)
  }
}
