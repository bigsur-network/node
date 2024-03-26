package io.rhonix.rholang.normalizer.env

/** Retrieve information about nesting structure during normalization. */
trait NestingReader {

  /**
   * This method represents the depth of pattern nesting.
   *
   * @return Int - This is an integer value that indicates how deeply a current location is nested within patterns.
   * For instance, if a location is not nested within any other patterns, the depth is 0.
   * If a pattern is location within one other pattern, the depth is 1.
   * If this other pattern inside another pattern, the depth is 2. And so on.
   */
  def patternDepth: Int

  /** Current processing is being executed within a receive pattern
   *  and this receive is not inside any other pattern */
  def insideTopLevelReceivePattern: Boolean

  /** Current processing is being executed within a bundle*/
  def insideBundle: Boolean
}
