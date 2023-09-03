package api

import cats.effect.kernel.Sync
import cats.effect.std.Dispatcher
import cats.effect.{Async, ExitCode, Resource}
import io.grpc.Server
import io.grpc.netty.NettyServerBuilder
import org.http4s.{HttpApp, HttpRoutes}
import org.http4s.blaze.server.BlazeServerBuilder
import rhonix.api.grpc.methods.All
import rhonix.api.grpc.serviceName
import sdk.api.FindApi
import sdk.syntax.all.*

import scala.concurrent.duration.DurationInt

object Servers {
  def rpc[F[_]: Async](port: Int, balanceApi: FindApi[F, String, Long]): Resource[F, Server] =
    Dispatcher.sequential[F].flatMap { dispatcher =>
      Resource.make {
        // Bind listener to the network adapter accepting API schema (service definition)
        Sync[F].delay {
          NettyServerBuilder
            .forPort(port)
            .addService(All(serviceName, (x: String) => dispatcher.unsafeRunSync(balanceApi.get(x))))
            .build
            .start()
        }
      }(server => Sync[F].delay(server.shutdown()))
    }

  def http[F[_]: Async](
    routes: HttpRoutes[F],
    httpPort: Int = 8080,
    host: String = "localhost",
  ): fs2.Stream[F, ExitCode] =
    BlazeServerBuilder[F] // TODO why NettyServerBuilder doesn't work?
      .bindHttp(httpPort, host)
      .withHttpApp(HttpApp[F](routes.orNotFound.run))
      .withIdleTimeout(1.seconds)
      .serve
}
