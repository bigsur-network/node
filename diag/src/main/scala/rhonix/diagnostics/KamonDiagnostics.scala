package rhonix.diagnostics

import cats.effect.{Resource, Sync}
import com.typesafe.config.Config
import kamon.Kamon
import kamon.metric.Timer
import kamon.tag.TagSet

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
  private def timer[F[_]: Sync](name: String, tags: Map[String, Any]): Resource[F, Unit] = {
    val start                  = Sync[F].delay(Kamon.timer(name).withTags(TagSet.from(tags)).start())
    def stop(t: Timer.Started) = Sync[F].delay(t.stop())
    Resource.make(start)(stop).map(_ => ())
  }

  def logTime[F[_]: Sync, T](f: => F[T])(name: String, tags: Map[String, Any] = Map()): F[T] = Sync[F].defer {
    if (Kamon.enabled()) timer(name, tags).surround(f) else f
  }
}
