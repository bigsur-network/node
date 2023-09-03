package rhonix.api.http.routes

import cats.effect.Sync
import cats.implicits.toFlatMapOps
import org.http4s.{EntityEncoder, HttpRoutes}
import sdk.api.FindApi
import sdk.syntax.all.*

object All {

  /** All routes */
  def apply[F[_]: Sync, T](findApi: FindApi[F, String, T])(implicit ei: EntityEncoder[F, T]): HttpRoutes[F] = {
    val dsl = org.http4s.dsl.Http4sDsl[F]
    import dsl.*

    HttpRoutes.of[F] { case GET -> Root / apiName / "balance" / id =>
      findApi.get(id).flatMap(x => Ok(x))
    }
  }
}
