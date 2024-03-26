package sdk.primitive

import cats.Show
import cats.effect.Sync
import cats.effect.kernel.{Async, Temporal}
import cats.effect.std.Console
import cats.syntax.all.*
import cats.effect.syntax.all.*
import fs2.Stream
import sdk.diag.Metrics
import sdk.syntax.all.diagEffectSdkSyntax

import scala.concurrent.duration.{DurationInt, FiniteDuration, MICROSECONDS, TimeUnit}

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

  /**
   * Analog to a Clock[F].timed that reports duration as a measurement.
   *
   * @param name  name for a measurement
   * @param units units to render string representing duration
   */
  def timedM(name: String, units: TimeUnit = MICROSECONDS)(implicit syncF: Sync[F], metrics: Metrics[F]): F[A] =
    x.timed.flatMap { case (duration, v) =>
      // this conversion is required to make sure AnyRef from Double is created correctly, since Double extends AnyVal
      val d: java.lang.Double = duration.toUnit(units)
      metrics.gauge(name, d).as(v)
    }

  /// https://typelevel.org/cats-effect/docs/core/starvation-and-tuning
  def cede(implicit F: Async[F]): F[A] = F.cede *> x.guarantee(F.cede)
}
