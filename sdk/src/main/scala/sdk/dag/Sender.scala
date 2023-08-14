package sdk.dag

trait Sender[F[_], T] {
  def sender[S](x: T): F[S]
}
