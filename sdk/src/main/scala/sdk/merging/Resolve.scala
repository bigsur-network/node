package sdk.merging

trait Resolve[F[_], T] {
  def resolve(x: IterableOnce[T]): F[(Set[T], Set[T])]
}

object Resolve {

  /** Simplest conflict resolver for iterator - partition into conflicting with anything and not. */
  def naive[T](target: IterableOnce[T], conflictsWithSome: T => Boolean): (IterableOnce[T], IterableOnce[T]) =
    target.iterator.partition(conflictsWithSome)
}
