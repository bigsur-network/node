package coop.rchain.rholang.normalizer2.util

import cats.effect.Sync
import cats.syntax.all.*
import io.rhonix.rholang.normalizer.env.NestingWriter

case class MockNestingWriter[F[_]: Sync]() extends NestingWriter[F] {
  private var insidePatternFlag: Boolean                = false
  private var insideTopLevelReceivePatternFlag: Boolean = false
  private var insideBundleFlag: Boolean                 = false

  override def withinPattern[R](withinReceive: Boolean)(scopeFn: F[R]): F[R] = for {
    insidePatternFlagRestore               <- Sync[F].delay(insidePatternFlag)
    _                                       = insidePatternFlag = true
    insideTopLevelReceivePatternFlagRestore = insideTopLevelReceivePatternFlag
    _                                       = insideTopLevelReceivePatternFlag = withinReceive
    res                                    <- scopeFn
    _                                       = insidePatternFlag = insidePatternFlagRestore
    _                                       = insideTopLevelReceivePatternFlag = insideTopLevelReceivePatternFlagRestore
  } yield res

  override def withinBundle[R](scopeFn: F[R]): F[R] = for {
    insideBundleFlagRestore <- Sync[F].delay(insideBundleFlag)
    _                        = insideBundleFlag = true
    res                     <- scopeFn
    _                        = insideBundleFlag = insideBundleFlagRestore
  } yield res

  def getInsidePatternFlag: Boolean                = insidePatternFlag
  def getInsideTopLevelReceivePatternFlag: Boolean = insideTopLevelReceivePatternFlag
  def getInsideBundleFlag: Boolean                 = insideBundleFlag
}
