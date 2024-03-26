package io.rhonix.rholang.normalizer

import cats.effect.Sync
import cats.syntax.all.*
import coop.rchain.rholang.interpreter.compiler.SourcePosition
import coop.rchain.rholang.interpreter.errors.{PatternReceiveError, TopLevelLogicalConnectivesNotAllowedError}
import io.rhonix.rholang.ast.rholang.Absyn.*
import io.rhonix.rholang.normalizer.env.NestingReader
import io.rhonix.rholang.types.ConnOrN
import sdk.syntax.all.*

object DisjunctionNormalizer {
  def normalizeDisjunction[F[_]: Sync: NormalizerRec](
    p: PDisjunction,
  )(implicit nestingInfo: NestingReader): F[ConnOrN] = {
    def pos = SourcePosition(p.line_num, p.col_num)

    if (nestingInfo.patternDepth > 0) // If we inside a pattern
      if (!nestingInfo.insideTopLevelReceivePattern)
        (p.proc_1, p.proc_2).nmap(NormalizerRec[F].normalize).mapN((left, right) => ConnOrN(Seq(left, right)))
      else {
        // These checks are necessary because top-level disjunctions and negations inside receive patterns should be prohibited.
        // Examples:
        // - Allowed code - conjunction inside pattern: `for (@{ x /\ Int } <- @"chan") { Nil }`
        // - Prohibited code - disjunction inside top-level pattern: `for (@{ String \/ Int } <- @"chan") { Nil }`
        // - Allowed code - disjunction inside pattern but not at the top level (there is another nested
        // pattern in the match): `for (@{ match "chan1" {String \/ Int => Nil}} <- @"chan2") { Nil }`
        PatternReceiveError(s"\\/ (disjunction) at $pos").raiseError
      }
    else
      TopLevelLogicalConnectivesNotAllowedError(s"\\/ (disjunction) at $pos").raiseError
  }
}
