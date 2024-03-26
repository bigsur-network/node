package io.rhonix.rholang.normalizer

import cats.effect.Sync
import cats.syntax.all.*
import coop.rchain.rholang.interpreter.compiler.SourcePosition
import coop.rchain.rholang.interpreter.errors.*
import io.rhonix.rholang.ast.rholang.Absyn.*
import io.rhonix.rholang.normalizer.env.NestingReader
import io.rhonix.rholang.types.ConnNotN

object NegationNormalizer {
  def normalizeNegation[F[_]: Sync: NormalizerRec](
    p: PNegation,
  )(implicit nestingInfo: NestingReader): F[ConnNotN] = {
    val pos = SourcePosition(p.line_num, p.col_num)

    if (nestingInfo.patternDepth > 0) // If we inside a pattern
      if (!nestingInfo.insideTopLevelReceivePattern)
        NormalizerRec[F].normalize(p.proc_).map(ConnNotN(_))
      else
        // These checks are necessary because top-level disjunctions and negations inside receive patterns should be prohibited.
        // Examples:
        // - Allowed code - conjunction inside pattern: `for (@{ x /\ Int } <- @"chan") { Nil }`
        // - Prohibited code - negation inside top-level receive pattern: `for (@{ ~Int } <- @"chan") { Nil }`
        // - Allowed code - negation inside pattern but not at the top level (there is another nested pattern in the match):
        // `for (@{ match "chan1" {~Int => Nil}} <- @"chan2") { Nil }`
        PatternReceiveError(s"~ (negation) at $pos").raiseError
    else
      TopLevelLogicalConnectivesNotAllowedError(s"~ (negation) at $pos").raiseError
  }
}
