package sdk.consensus

trait ConflictResolution[F[_], T, A, R] {
  def accepted(x: T): F[A]
  def rejected(x: T): F[R]
}
