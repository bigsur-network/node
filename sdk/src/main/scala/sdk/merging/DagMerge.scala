package sdk.merging

trait DagMerge[F[_], M] {
  def between(ceil: Set[M], floor: Set[M]): F[Iterator[M]]
  def latestFringe(target: Set[M]): F[Set[M]]
}
