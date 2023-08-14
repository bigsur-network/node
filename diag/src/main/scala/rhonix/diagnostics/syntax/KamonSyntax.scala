package rhonix.diagnostics.syntax

import cats.effect.kernel.Sync
import rhonix.diagnostics.KamonDiagnostics

final class KamonSyntax[F[_], T](private val f: F[T]) extends AnyVal {
  def kamonTimer(name: String, tags: Map[String, Any] = Map())(implicit syncF: Sync[F]): F[T] =
    KamonDiagnostics.logTime(f)(name, tags)
}

object KamonSyntax {
  implicit def kamonSyntax[F[_]: Sync, T](f: F[T]): KamonSyntax[F, T] = new KamonSyntax[F, T](f)
}
