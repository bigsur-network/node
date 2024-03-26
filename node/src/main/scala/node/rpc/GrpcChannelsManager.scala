package node.rpc

import cats.effect.{Async, Ref, Resource}
import cats.syntax.all.*
import io.grpc.*
import io.grpc.netty.NettyChannelBuilder
import sdk.data.HostWithPort

trait GrpcChannelsManager[F[_]] {
  def get(hostWithPort: HostWithPort): F[ManagedChannel]
}

object GrpcChannelsManager {
  def apply[F[_]: Async]: Resource[F, GrpcChannelsManager[F]] = Resource
    .make(Ref.of(Map.empty[HostWithPort, ManagedChannel]))(_.get.map(_.values.foreach(_.shutdown())))
    .map { channelsRef =>
      new GrpcChannelsManager[F] {
        def get(hostWithPort: HostWithPort): F[ManagedChannel] = channelsRef.modify { st =>
          st.get(hostWithPort) match {
            case Some(channel) => (st, channel)
            case None          =>
              val newChannel = NettyChannelBuilder.forAddress(hostWithPort.host, hostWithPort.port).usePlaintext().build
              val newSt      = st + (hostWithPort -> newChannel)
              newSt -> newChannel
          }
        }
      }
    }
}
