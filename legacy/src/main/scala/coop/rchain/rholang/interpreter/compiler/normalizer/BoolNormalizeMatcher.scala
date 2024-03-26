package coop.rchain.rholang.interpreter.compiler.normalizer

import io.rhonix.rholang.ast.rholang.Absyn.{BoolFalse, BoolLiteral, BoolTrue}
import io.rhonix.rholang.types.GBoolN

object BoolNormalizeMatcher {
  def normalizeMatch(b: BoolLiteral): GBoolN =
    b match {
      case _: BoolTrue  => GBoolN(true)
      case _: BoolFalse => GBoolN(false)
    }
}
