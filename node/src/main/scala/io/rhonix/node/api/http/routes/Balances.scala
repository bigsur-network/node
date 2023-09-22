package io.rhonix.node.api.http.routes

import cats.effect.Sync
import cats.implicits.toFlatMapOps
import cats.syntax.all.*
import io.rhonix.node.api.http.ApiPath
import org.http4s.{EntityEncoder, HttpRoutes}
import sdk.api.BalancesApi
import sdk.codecs.Base16
import sdk.hashing.Blake2b256Hash

import scala.util.Try

object Balances {
  def apply[F[_]: Sync, T](
    balanceApi: (Blake2b256Hash, Int) => F[T],
  )(implicit ei: EntityEncoder[F, T]): HttpRoutes[F] = {
    val dsl = org.http4s.dsl.Http4sDsl[F]
    import dsl.*

    HttpRoutes.of[F] { case GET -> ApiPath / BalancesApi.MethodName / hash / wallet =>
      (Try(wallet.toInt), Base16.decode(hash).map(Blake2b256Hash(_)))
        .traverseN { case (w, h) => balanceApi(h, w) }
        .flatMap(_.map(Ok(_)).getOrElse(NotFound()))
    }
  }
}
