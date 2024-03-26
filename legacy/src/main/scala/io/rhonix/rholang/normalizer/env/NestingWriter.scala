package io.rhonix.rholang.normalizer.env

/** Preserve information about nesting structure during normalization. */
trait NestingWriter[F[_]] {

  /** Run scopeFn with a note that this is a pattern
   * @param withinReceive Flag is necessary for normalizing the connectives.
   * Since we cannot rely on a specific pattern matching order, we cannot use patterns
   * separated by \/ to bind any variables in the top-level receive.
   * */
  def withinPattern[R](withinReceive: Boolean)(scopeFn: F[R]): F[R]

  /** Run scopeFn with a note that this is a bundle */
  def withinBundle[R](scopeFn: F[R]): F[R]
}

object NestingWriter {
  def apply[F[_]](implicit instance: NestingWriter[F]): instance.type = instance
}
