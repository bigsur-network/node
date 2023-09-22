package sdk.primitive

import cats.Show
import cats.effect.implicits.monadCancelOps_
import cats.effect.kernel.{Async, Temporal}
import cats.effect.std.Console
import cats.implicits.toShow
import fs2.Stream
import cats.syntax.all.*

import scala.concurrent.duration.{DurationInt, FiniteDuration}

trait EffectSyntax {
  implicit def effectSyntax[F[_], A](x: F[A]): EffectOps[F, A] = new EffectOps[F, A](x)
}

final class EffectOps[F[_], A](private val x: F[A]) extends AnyVal {

  /// Live update of the data in the console
  def showAnimated(samplingTime: FiniteDuration = 150.millisecond)(implicit
    F: Temporal[F],
    showA: Show[A],
    console: Console[F],
  ): Stream[F, Unit] =
    // \u001b[2J - clear screen
    Stream.repeatEval(x).metered(samplingTime).map(v => s"\u001b[2J${v.show}").printlns

  def cede(implicit F: Async[F]): F[A] = F.cede *> x.guarantee(F.cede)
}
