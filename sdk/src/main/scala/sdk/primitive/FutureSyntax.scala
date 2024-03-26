package sdk.primitive

import cats.effect.{Async, Sync}

import scala.concurrent.Future

trait FutureSyntax {
  implicit def sdkSyntaxFuture[A](a: => Future[A]): FutureOps[A] = new FutureOps(() => a)
}

final class FutureOps[A](private val a: () => Future[A]) extends AnyVal {
  def asEffect[F[_]: Async]: F[A] = Async[F].fromFuture(Sync[F].delay(a()))
}
