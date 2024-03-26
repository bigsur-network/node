package io.rhonix.rholang.normalizer.env

trait BoundVarScope[F[_]] {

  /** Run function within an empty bound variables scope (preserving history). */
  def withNewBoundVarScope[R](scopeFn: F[R]): F[R]

  /** Run functions within a copy of current bound variables scope (preserving history). */
  def withCopyBoundVarScope[R](scopeFn: F[R]): F[R]
}

object BoundVarScope {
  def apply[F[_]](implicit instance: BoundVarScope[F]): BoundVarScope[F] = instance
}
