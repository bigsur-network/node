package io.rhonix.node.api.http.routes

import cats.effect.Sync
import cats.syntax.all.*
import io.rhonix.node.api.http.ApiPath
import org.http4s.{EntityEncoder, HttpRoutes}
import sdk.api.*
import sdk.codecs.Base16
import sdk.hashing.Blake2b256Hash

import scala.util.Try

object All {
  def apply[F[_]: Sync, T](
    blockApi: BlockDbApi[F],
    deployApi: BlockDeploysDbApi[F],
    balanceApi: (Blake2b256Hash, Int) => F[T],
  )(implicit
    ei: EntityEncoder[F, T],
  ): HttpRoutes[F] = {
    val dsl = org.http4s.dsl.Http4sDsl[F]
    import dsl.*

    // This derives encoders for response types
    import io.circe.generic.auto.*
    import org.http4s.circe.CirceEntityCodec.circeEntityEncoder

    HttpRoutes.of[F] {
      case GET -> ApiPath / BlockDbApi.MethodName / id        =>
        val hash = java.util.Base64.getDecoder.decode(id)
        blockApi.getByHash(hash).flatMap(_.map(Ok(_)).getOrElse(NotFound()))
      case GET -> ApiPath / BlockDeploysDbApi.MethodName / id =>
        deployApi.getByBlock(id.toLong).flatMap(Ok(_))
    } <+> Balances[F, T](balanceApi)
  }
}
