package io.rhonix.rholang.normalizer.envimpl

import cats.effect.Sync
import cats.syntax.all.*
import io.rhonix.rholang.normalizer.env.NestingWriter
import io.rhonix.rholang.normalizer.syntax.all.*

final case class NestingWriterImpl[F[_]: Sync](
  patternInfo: HistoryChain[(Int, Boolean)],
  bundleInfo: HistoryChain[Boolean],
) extends NestingWriter[F] {
  override def withinPattern[R](inReceive: Boolean)(scopeFn: F[R]): F[R] =
    for {
      incrementedDepth <- Sync[F].delay(patternInfo.current()._1 + 1)
      fRes             <- patternInfo.runWithNewDataInChain(scopeFn, (incrementedDepth, inReceive))
    } yield fRes

  override def withinBundle[R](scopeFn: F[R]): F[R] =
    bundleInfo.runWithNewDataInChain(scopeFn, true)
}
