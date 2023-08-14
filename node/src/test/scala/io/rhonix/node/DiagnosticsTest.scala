package io.rhonix.node

import cats.effect.{IO, IOApp}
import rhonix.diagnostics.KamonDiagnostics
import rhonix.diagnostics.syntax.KamonSyntax.kamonSyntax

import scala.concurrent.duration.DurationInt

object DiagnosticsTest extends IOApp.Simple {
  val diagResource           = for {
    kmn <- KamonDiagnostics.kamonResource[IO]()
  } yield (kmn, ())
  override def run: IO[Unit] = diagResource.use { case (idb, kmn) =>
    def f = IO.sleep(1.second)
    f.kamonTimer("Dummy function").replicateA_(100)
  }
}
