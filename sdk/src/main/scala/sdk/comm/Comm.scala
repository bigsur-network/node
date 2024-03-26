package sdk.comm

/**
 * Interface for communication with peers.
 * @tparam A inbound message type
 * @tparam B outbound message type
 */
trait Comm[F[_], A, B] {
  def broadcast(msg: A): F[Unit]
  def receiver: fs2.Stream[F, B]
}
