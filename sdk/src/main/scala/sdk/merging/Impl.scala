package sdk.merging

import cats.data.Kleisli
import cats.kernel.Monoid
import cats.syntax.all.*
import cats.{Id, Monad}
class Impl {
//
//trait Finality[F[_], T] {
//  // finality should have total order
//  val tTotalOrder: Comparable[T]
//}
//  /** Simplest conflict resolver for iterator - partition into conflicting with anything and not. */
//  def resolveForCollectionNaive[T](
//    conflictsWithSome: ConflictsWithSome[Id, T],
//  ): Resolve[Id, IterableOnce[T], (IterableOnce[T], IterableOnce[T])] = {
//    val f = (x: IterableOnce[T]) => x.iterator.partition(conflictsWithSome.run)
//    Kleisli(f)
//  }
//
//  /** Reject one side of conflict and persis the winner. TODO */
//  def resolveForCollectionPickWinner[T](
//    conflictsWith: ConflictsWith[Id, T],
//  ): Resolve[Id, IterableOnce[T], IterableOnce[T]] = ???
//
//  /** Sophisticated stateful resolver. TODO */
//  def resolveForSetSomeFancyLogicWithCashAndSuperFastOccupy100TBMemory[T, S](
//    conflictsWith: ConflictsWith[Id, T],
//    giantState: S,
//  ): Resolve[Id, IterableOnce[T], IterableOnce[T]] =
//    ???
//
//  def betweenSetForSeenMap[T](seenMap: Map[T, Set[T]]): Between[Id, Set[T], Set[T]] = {
//    val f = (ceil: Set[T], floor: Set[T]) => (ceil.flatMap(seenMap) ++ ceil -- floor.flatMap(seenMap)).pure[Id]
//    Kleisli[Id, (Set[T], Set[T]), Set[T]](f.tupled)
//  }
//
//  def betweenIteratorForSeenMap[F[_]: Monad, T](seenMap: Map[T, Set[T]]): Between[Id, Set[T], Iterator[T]] =
//    betweenSetForSeenMap[T](seenMap) andThen (_.iterator)
//
//  def betweenForSenderWithSeq[F[_]: Monad, T, S](
//    seqWithSender: T => F[(S, Long)],
//    lookup: (S, Long) => T,
//  ): Between[Id, Set[T], Iterator[T]] = {
//    val f = (ceil: Set[T], floor: Set[T]) =>
//      for {
//        c <- ceil.map(seqWithSender).toList.sequence.map(_.toMap)
//        f <- floor.map(seqWithSender).toList.sequence.map(_.toMap)
//        r  = c.iterator.flatMap { case (k, up) => (f(k) to up).iterator.map(k -> _) }
//      } yield r.map(lookup.tupled)
//    Kleisli(f.tupled)
//  }
//
//  /** Merge by folding some collection of monoids. */
//  def mergeForMonoidCollections[T: Monoid]: Merge[Id, IterableOnce[T], T] = {
//    val f = (x: IterableOnce[T]) => Monoid[T].combineAll(x)
//    Kleisli(f)
//  }
//
//  /** More efficient stateful merge. TODO */
//  def mergeWithCaching[T: Monoid, S](state: S): Merge[Id, Set[T], T] = ???

//  trait Conflicting[T] {
//    def conflictsWith(x: T, y: T): Boolean
//
//    def conflictsWithAny(x: T): Boolean
//  }
//
//  trait UnorderedMergeable[C[_], T] {
//    implicit val conflictingForT: Conflicting[T]
//    implicit val unorderedFoldableForC: UnorderedFoldable[C]
//    implicit val v: CommutativeMonoid[T]
//
//    def merge(scope: C[T]): T = UnorderedFoldable[C].unorderedFold(scope)
//  }
//
//  object Merge {
//    //  def merge[F[_], Scope[_]: UnorderedFoldable, T: CommutativeMonoid](scope: Scope[T])(implicit
//    //    conflictingForT: Conflicting[T],
//    //  ) = UnorderedFoldable[Scope].unorderedFold(scope)
//  }

}
