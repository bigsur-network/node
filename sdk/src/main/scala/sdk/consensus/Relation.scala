package sdk.consensus

trait Relation[F[_], T] {
  def conflicts(l: T, r: T): F[Boolean]
  def depends(x: T, on: T): F[Boolean]
}
