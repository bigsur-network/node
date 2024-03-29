package coop.rchain.rholang.interpreter.compiler.normalizer.processes

import cats.syntax.all.*
import coop.rchain.rholang.interpreter.compiler.{NameVisitOutputs, ProcVisitInputs}
import coop.rchain.rholang.interpreter.errors.{InterpreterError, PatternReceiveError}
import io.rhonix.rholang.types.{ConnNotN, ConnOrN}

object Utils {
  def failOnInvalidConnective(
    input: ProcVisitInputs,
    nameRes: NameVisitOutputs,
  ): Either[InterpreterError, NameVisitOutputs] =
    if (input.boundMapChain.depth == 0) {
      Either
        .fromOption(
          nameRes.freeMap.connectives
            .collectFirst {
              case (_: ConnOrN, sourcePosition)  =>
                PatternReceiveError(s"\\/ (disjunction) at $sourcePosition")
              case (_: ConnNotN, sourcePosition) =>
                PatternReceiveError(s"~ (negation) at $sourcePosition")
            },
          nameRes,
        )
        .swap
    } else Right(nameRes)
}
