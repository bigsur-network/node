package io.rhonix.rholang.normalizer.envimpl

import io.rhonix.rholang.normalizer.env.NestingReader

final case class NestingReaderImpl(
  patternInfo: HistoryChain[(Int, Boolean)],
  bundleInfo: HistoryChain[Boolean],
) extends NestingReader {
  override def patternDepth: Int                     = patternInfo.current()._1
  override def insideTopLevelReceivePattern: Boolean = patternInfo.current()._2
  override def insideBundle: Boolean                 = bundleInfo.current()
}
