package sdk.dag

trait Parents[F[_], T] {
  def parents(x: T): F[Set[T]]
}
