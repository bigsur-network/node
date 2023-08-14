package sdk.debug

import cats.effect.kernel.{Async, Ref}
import cats.effect.std.Queue
import cats.effect.{IO, Sync}
import cats.syntax.all.*
import fs2.Pipe

import scala.concurrent.duration.{Duration, DurationInt, FiniteDuration}

object Stopwatch {

  // TODO: this is temporary solution to measure and log duration.
  def time[F[_]: Sync, A](log: String => F[Unit])(tag: String)(block: => F[A]): F[A] =
    for {
      t0 <- Sync[F].delay(System.nanoTime)
      a  <- block
      t1  = System.nanoTime
      m   = Duration.fromNanos(t1 - t0)
      _  <- log(s"$tag [${showTime(m)}]")
    } yield a

  def durationRaw[F[_]: Sync, A](block: => F[A]): F[(A, FiniteDuration)] =
    for {
      t0 <- Sync[F].delay(System.nanoTime)
      a  <- block
      t1  = System.nanoTime
      m   = Duration.fromNanos(t1 - t0)
    } yield (a, m)

  def duration[F[_]: Sync, A](block: => F[A]): F[(A, String)] =
    durationRaw(block).map(_.map(showTime))

  def timed[A](block: => A): (A, FiniteDuration) = {
    val t0 = System.nanoTime
    val a  = block
    val t1 = System.nanoTime
    (a, Duration.fromNanos(t1 - t0))
  }

  def profile[A](block: => A): (A, String) = {
    val t0 = System.nanoTime
    val a  = block
    val t1 = System.nanoTime
    val m  = Duration.fromNanos(t1 - t0)
    (a, showTime(m))
  }

  def profileLog[A](block: => A): A = {
    val t0 = System.nanoTime
    val a  = block
    val t1 = System.nanoTime
    val m  = Duration.fromNanos(t1 - t0)
    println(showTime(m))
    a
  }

  def profileLogF[F[_]: Sync, A](block: => F[A]): F[A] = duration(block).map { case (r, t) =>
    println(t)
    r
  }

  def showTime(d: FiniteDuration): String = {
    val ns   = 1d
    val ms   = 1e6 * ns
    val sec  = 1000 * ms
    val min  = 60 * sec
    val hour = 60 * min
    val m    = d.toNanos
    if (m >= hour) s"${m / hour} hour"
    else if (m >= min) s"${m / min} min"
    else if (m >= sec) s"${m / sec} sec"
    else if (m >= ms) s"${m / ms} ms"
    else s"${m / 1e6d} ms"
  }

//  def average[F[_]: Async](duration: FiniteDuration): Pipe[F, FiniteDuration, FiniteDuration] =
//    (in: fs2.Stream[F, FiniteDuration]) => {
//      val acc  = Ref.unsafe[F, List[FiniteDuration]](List())
//      val pull = in.evalTap(x => acc.update(_ :+ x))
//      val out  = fs2.Stream
//        .awakeEvery(duration)
//        .evalTap(_ => acc.get.map(println))
//        .evalMap(_ =>
//          acc
//            .getAndUpdate(_ => List())
//            .map(x => if (x.nonEmpty) Duration.fromNanos(x.map(_.toNanos).sumAll / x.size) else Duration.Zero),
//        )
//      out concurrently pull
//    }
//
//  import cats.effect.unsafe.implicits.global
//  val q = Queue.unbounded[IO, FiniteDuration].unsafeRunSync()
//  locally {
//    val _ = fs2.Stream.fromQueueUnterminated(q).through(average(1.second)).printlns.compile.drain.start.unsafeRunSync()
//  }

//  def profileLogAverage[A](block: => A): A = {
//    val (x, t) = timed(block)
//    q.offer(t).unsafeRunSync()
//    x
//  }

  trait DebugSyntax {
    final class Log[A](f: => A) {
      def logTime: A = profileLog(f)
//      def logTimeAverage: A = profileLogAverage(f)
    }

    final class LogF[F[_]: Sync, A](f: => F[A]) {
      def logTimeF: F[A] = profileLogF(f)
    }
    implicit def logTime[A](f: => A) = new Log[A](f)
    implicit def logTimeF[F[_]: Sync, A](f: => F[A]) = new LogF[F, A](f)
  }
}
