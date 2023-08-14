package sdk.dag

trait DagNode[F[_], T, M, S] {
  def parents(x: T): F[Set[M]]
  def sender(x: T): F[S]
}
