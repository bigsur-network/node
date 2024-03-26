package node.rpc.syntax

import cats.effect.Async
import cats.syntax.all.*
import cats.{Eval, Monad}
import dproc.data.Block
import io.grpc.MethodDescriptor
import node.comm.PeerTable
import node.rpc.{GrpcChannelsManager, GrpcClient, GrpcMethod}
import sdk.api.{BlockEndpoint, BlockHashEndpoint, LatestBlocksEndpoint}
import sdk.codecs.Serialize
import sdk.comm.Peer
import sdk.data.{BalancesDeploy, HostWithPort}
import sdk.primitive.ByteArray
import sdk.serialize.auto.*

trait GrpcClientSyntax {
  implicit def grpcClientSyntax[F[_]](client: GrpcClient[F]): GrpcClientOps[F] = new GrpcClientOps[F](client)
}

final class GrpcClientOps[F[_]](private val client: GrpcClient[F]) extends AnyVal {

  /** Call specifying endpoint with host and port. */
  private def callMethod[Req, Resp](
    method: MethodDescriptor[Req, Resp],
    msg: Req,
    hostWithPort: HostWithPort,
  )(implicit
    channelsManager: GrpcChannelsManager[F],
    monadF: Monad[F],
  ): F[Resp] = channelsManager.get(hostWithPort).flatMap(client.call(method, msg, _))

  private def callEndpoint[Req, Resp](endpoint: String, msg: Req, peerAddress: HostWithPort)(implicit
    F: Monad[F],
    gcm: GrpcChannelsManager[F],
    sA: Serialize[Eval, Req],
    sB: Serialize[Eval, Resp],
  ): F[Resp] = callMethod[Req, Resp](GrpcMethod[Req, Resp](endpoint), msg, peerAddress)

  def resolveBlock(hash: ByteArray, peerAddress: HostWithPort)(implicit
    F: Async[F],
    c: GrpcChannelsManager[F],
  ): F[Option[Block.WithId[ByteArray, ByteArray, BalancesDeploy]]] =
    callEndpoint[ByteArray, Option[Block.WithId[ByteArray, ByteArray, BalancesDeploy]]](
      BlockEndpoint,
      hash,
      peerAddress,
    )

  def getLatestBlocks(peerAddress: HostWithPort)(implicit
    F: Async[F],
    c: GrpcChannelsManager[F],
  ): F[List[ByteArray]] =
    callEndpoint[Unit, List[ByteArray]](LatestBlocksEndpoint, (), peerAddress)

  def reportBlockHash(hash: ByteArray, suggestedResolver: HostWithPort, reportTo: HostWithPort)(implicit
    F: Async[F],
    c: GrpcChannelsManager[F],
  ): F[Boolean] =
    callEndpoint[(ByteArray, HostWithPort), Boolean](
      BlockHashEndpoint,
      (hash, suggestedResolver),
      reportTo,
    )

  def broadcastBlockHash(hash: ByteArray, resolver: HostWithPort)(implicit
    F: Async[F],
    c: GrpcChannelsManager[F],
    peerManager: PeerTable[F, (String, Int), Peer],
  ): F[Unit] =
    for {
      peers <- peerManager.all.map(_.values.filterNot(_.isSelf).toList)
      _     <- peers.traverse { peer =>
                 val socket = HostWithPort(peer.host, peer.port)
                 reportBlockHash(hash, resolver, socket)
               }
    } yield ()
}
