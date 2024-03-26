package node.rpc

import cats.effect.kernel.Async
import cats.effect.std.Dispatcher
import cats.effect.{Resource, Sync}
import cats.syntax.all.*
import dproc.data.Block
import io.grpc.*
import io.grpc.netty.NettyServerBuilder
import sdk.serialize.auto.*
import sdk.api.{BlockEndpoint, BlockHashEndpoint, LatestBlocksEndpoint}
import sdk.data.{BalancesDeploy, HostWithPort}
import sdk.log.Logger.*
import sdk.primitive.ByteArray

object GrpcServer {

  def apply[F[_]: Async](
    port: Int,
    hashRcvF: (ByteArray, HostWithPort) => F[Boolean],
    blockResolve: ByteArray => F[Option[Block.WithId[ByteArray, ByteArray, BalancesDeploy]]],
    latestBlocks: Unit => F[List[ByteArray]],
  ): Resource[F, Server] =
    Dispatcher.sequential[F].flatMap { dispatcher =>
      val serviceDefinition: ServerServiceDefinition = ServerServiceDefinition
        .builder(sdk.api.RootPathString)
        .addMethod(
          GrpcMethod[(ByteArray, HostWithPort), Boolean](BlockHashEndpoint),
          GrpcMethodHandler(hashRcvF.tupled, dispatcher),
        )
        .addMethod(
          GrpcMethod[ByteArray, Option[Block.WithId[ByteArray, ByteArray, BalancesDeploy]]](BlockEndpoint),
          GrpcMethodHandler(blockResolve, dispatcher),
        )
        .addMethod(
          GrpcMethod[Unit, List[ByteArray]](LatestBlocksEndpoint),
          GrpcMethodHandler(latestBlocks, dispatcher),
        )
        .build()

      Resource.make(
        Sync[F].delay {
          NettyServerBuilder
            .forPort(port)
            .addService(serviceDefinition)
            .build
            .start()
        } <* logInfoF(s"RPC server started on port $port."),
      )(server => Sync[F].delay(server.shutdown()) *> logInfoF(s"RPC server stopped."))
    }
}
