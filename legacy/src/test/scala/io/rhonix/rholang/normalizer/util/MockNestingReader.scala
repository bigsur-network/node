package coop.rchain.rholang.normalizer2.util

import io.rhonix.rholang.normalizer.env.NestingReader

case class MockNestingReader(
  patternDepthInit: Int,
  insideTopLevelReceivePatternInit: Boolean,
  insideBundleInit: Boolean,
) extends NestingReader {
  override def patternDepth: Int                     = patternDepthInit
  override def insideTopLevelReceivePattern: Boolean = insideTopLevelReceivePatternInit
  override def insideBundle: Boolean                 = insideBundleInit
}
