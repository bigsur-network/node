package slick.syntax

import cats.effect.kernel.Async
import cats.syntax.all.*
import org.postgresql.util.{PSQLException, PSQLState}
import slick.SlickDb
import slick.dbio.{DBIOAction, Effect, NoStream}

trait DBIOActionRunSyntax {
  implicit def DBIOActionRunSyntax[R, S <: NoStream, E <: Effect](
    x: DBIOAction[R, S, E],
  ): DBIOSyntaxOps[R, S, E] = new DBIOSyntaxOps[R, S, E](x)
}

final class DBIOSyntaxOps[R, +S <: NoStream, -E <: Effect](val x: DBIOAction[R, S, E]) extends AnyVal {
  def run[F[_]](implicit F: Async[F], slickDb: SlickDb): F[R] = slickDb
    .run(x)
    // This error handling is to ensure that the action is run the second time if the first time it failed with
    // UNIQUE_VIOLATION. This is to make concurrent inserts work for data that requires lookups, since Slick does not
    // provide tools that raw SQL has to offer in such scenarios (forUpdate did not work, onConflict is not supported).
    .handleErrorWith {
      case ex: PSQLException if ex.getSQLState == PSQLState.UNIQUE_VIOLATION.getState => slickDb.run(x)
      case error                                                                      => F.raiseError(error)
    }
}
