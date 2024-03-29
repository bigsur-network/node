package coop.rchain.rholang.interpreter.compiler.normalizer.processes

import cats.effect.Sync
import cats.syntax.all.*
import coop.rchain.models.Par
import io.rhonix.rholang.ast.rholang.Absyn.PNegation
import coop.rchain.rholang.interpreter.compiler.ProcNormalizeMatcher.normalizeMatch
import coop.rchain.rholang.interpreter.compiler.{FreeMap, ProcVisitInputs, ProcVisitOutputs, SourcePosition}
import io.rhonix.rholang.types.{ConnNotN, NilN, ParN}

object PNegationNormalizer {
  def normalize[F[_]: Sync](p: PNegation, input: ProcVisitInputs)(implicit
    env: Map[String, Par],
  ): F[ProcVisitOutputs] =
    normalizeMatch[F](
      p.proc_,
      ProcVisitInputs(NilN, input.boundMapChain, FreeMap.empty),
    ).map { bodyResult =>
      val conn = ConnNotN(bodyResult.par)
      ProcVisitOutputs(
        ParN.combine(input.par, conn),
        input.freeMap.addConnective(
          conn,
          SourcePosition(p.line_num, p.col_num),
        ),
      )
    }
}
