package node

import cats.effect.std.Queue
import cats.effect.{Async, Ref}
import cats.syntax.all.*
import dproc.data.Block
import fs2.Stream
import node.rpc.syntax.all.grpcClientSyntax
import node.rpc.{GrpcChannelsManager, GrpcClient}
import sdk.data.{BalancesDeploy, HostWithPort}
import sdk.log.Logger.*
import sdk.primitive.ByteArray

import java.net.{InetSocketAddress, SocketAddress}

final case class BlockResolver[F[_]](
  // request to resolve a block
  in: (ByteArray, HostWithPort) => F[Boolean],
  // stream of resolved blocks
  out: Stream[F, Block.WithId[ByteArray, ByteArray, BalancesDeploy]],
  // block is consumed by upstream and can be removed from resolver state
  done: ByteArray => F[Unit],
)

object BlockResolver {

  trait ResolveStatus
  object Requested extends ResolveStatus
  object Resolved  extends ResolveStatus

  final case class ST(statuses: Map[ByteArray, ResolveStatus]) {
    def request(hash: ByteArray): (ST, Boolean) =
      if (statuses.exists(_._1 == hash)) this    -> false
      else ST(statuses.updated(hash, Requested)) -> true

    def resolve(hash: ByteArray): (ST, Boolean) =
      if (statuses.get(hash).contains(Requested)) this -> false
      else ST(statuses.updated(hash, Resolved))        -> true

    def done(hash: ByteArray): (ST, Boolean) =
      if (statuses.get(hash).contains(Resolved)) ST(statuses - hash) -> true
      else this                                                      -> false
  }

  def apply[F[_]: Async: GrpcChannelsManager]: F[BlockResolver[F]] = for {
    st   <- Ref.of[F, ST](ST(Map.empty))
    inQ  <- Queue.unbounded[F, (ByteArray, HostWithPort)]
    outQ <- Queue.unbounded[F, Block.WithId[ByteArray, ByteArray, BalancesDeploy]]
  } yield {
    val resolveStream = Stream
      .fromQueueUnterminated(inQ)
      .evalFilter { case (hash, _) => st.modify(_.request(hash)) }
      .parEvalMapUnorderedUnbounded { case (hash, peer) => GrpcClient[F].resolveBlock(hash, peer) }
      .evalTap(_.traverse(outQ.offer))

    val in                 = inQ.offer(_: ByteArray, _: HostWithPort).as(true)
    val out                = Stream.eval(logDebugF("BlockResolver started.")) *>
      Stream.fromQueueUnterminated(outQ) concurrently resolveStream
    def done(x: ByteArray) = st.modify(_.done(x)).flatMap { ok =>
      new Exception(s"Block $x is not resolved yet but done called").raiseError.whenA(!ok)
    }

    new BlockResolver[F](in, out, done)
  }
}
