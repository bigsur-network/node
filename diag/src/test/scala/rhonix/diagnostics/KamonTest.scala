package rhonix.diagnostics

import cats.effect.kernel.Async
import cats.effect.{IO, IOApp}
import cats.syntax.all.*
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime
import rhonix.diagnostics.syntax.KamonSyntax.kamonSyntax

object KamonTest extends IOApp.Simple {
  def rec[F[_]: Async](n: Int): F[Unit] =
    if (n == 0) ().pure
    else Async[F].sleep(100.millis) *> rec(n - 1).kamonTrace("step", "recursion")

  override def run: IO[Unit] =
    // Application has to use this resource to initialize Kamon
    KamonDiagnostics.kamonResource[IO]().surround {
      // 100 traces each containing 10 nested spans should appear in the Jaeger UI
      // look at resources/reference.conf for Kamon configuration
      rec[IO](10).replicateA_(100)
    }
}
