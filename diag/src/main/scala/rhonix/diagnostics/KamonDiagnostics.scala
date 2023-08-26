package rhonix.diagnostics

import cats.effect.kernel.Outcome
import cats.effect.{Resource, Sync}
import com.typesafe.config.Config
import kamon.Kamon
import kamon.metric.Timer
import kamon.tag.TagSet
import kamon.trace.Span

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

object KamonDiagnostics {

  /**
   * Program having access to Kamon has to be run surrounded by this resource.
   */
  def kamonResource[F[_]: Sync](configOpt: Option[Config] = None): Resource[F, Unit] = {
    val stopTimeout = 2.second
    val start       = Sync[F].delay(configOpt.map(Kamon.init).getOrElse(Kamon.init()))
    val stop        = Sync[F].delay(Await.result(Kamon.stop(), stopTimeout))
    Resource.make(start)(_ => stop)
  }

  /**
   * @param name name of a timer
   * @param tags String or Long or Boolean, as per Kamon documentation. Others will be ignored.
   * @tparam F
   * @return
   */
  private def timer[F[_]: Sync](name: String, tags: Map[String, Any]): Resource[F, Timer.Started] = {
    val start                  = Sync[F].delay(Kamon.timer(name).withTags(TagSet.from(tags)).start())
    def stop(t: Timer.Started) = Sync[F].delay(t.stop())
    Resource.make(start)(stop)
  }

  // Adaptation of Kamon's function span(operationName: String, component: String)(f: => A): A to cats-effect
  private def span[F[_]: Sync, A](opName: String, component: String)(f: F[A]) = Sync[F].defer {
    val span  = Kamon
      .spanBuilder(opName)
      .kind(Span.Kind.Internal)
      .tagMetrics(Span.TagKeys.Component, component)
      .start()
    val ctx   = Kamon.currentContext().withEntry(Span.Key, span)
    val scope = Kamon.storeContext(ctx)

    Sync[F].guaranteeCase(f) {
      case Outcome.Errored(err) => Sync[F].delay(span.fail(err.getMessage, err).finish())
      case _                    => Sync[F].delay { span.finish(); scope.close() }
    }
  }

  def logTime[F[_]: Sync, T](f: => F[T])(name: String, tags: Map[String, Any] = Map()): F[T] = Sync[F].defer {
    if (Kamon.enabled()) timer(name, tags).surround(f) else f
  }

  def span[F[_]: Sync, T](f: => F[T])(opName: String, component: String): F[T] = Sync[F].defer {
    if (Kamon.enabled()) span(opName, component)(f) else f
  }
}
