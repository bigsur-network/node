//package sdk.meld
//
//import cats.Monad
//import cats.kernel.Monoid
//
//trait Meld[View[_]] {
//  def finalView(t: T): View[T]    = ???
//  def conflictView(t: T): View[T] = ???
//}
//
//object Meld {
////
//  final case class ConflictResolution[View[_]](accepted: View, rejected: View)
//
//  def resolve[F[_]: Monad, View[_]: Monoid, T](
//    target: View[T],
//    finality: ConflictResolution[View],
//    meld: Meld[T, View],
//  ) = {
//
//    def dependingOnRejected(conflictView: View[T]): View[T] = ???
//
//    def resolve(target: View[T]): F[ConflictResolution[View]] = ???
//
//    for {
//      (rj1, cs) <- target(depends on rejected)
//      rj2       <- resolve(rj1)
//    } yield ConflictResolution.empty[View]
//  }
//}
