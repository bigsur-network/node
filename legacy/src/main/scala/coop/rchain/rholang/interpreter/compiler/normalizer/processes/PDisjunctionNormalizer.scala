package coop.rchain.rholang.interpreter.compiler.normalizer.processes

import cats.effect.Sync
import cats.syntax.all.*
import coop.rchain.models.Par
import io.rhonix.rholang.ast.rholang.Absyn.PDisjunction
import coop.rchain.rholang.interpreter.compiler.ProcNormalizeMatcher.normalizeMatch
import coop.rchain.rholang.interpreter.compiler.{FreeMap, ProcVisitInputs, ProcVisitOutputs, SourcePosition}
import io.rhonix.rholang.types.{ConnOrN, NilN, ParN}

object PDisjunctionNormalizer {
  def normalize[F[_]: Sync](p: PDisjunction, input: ProcVisitInputs)(implicit
    env: Map[String, Par],
  ): F[ProcVisitOutputs] =
    for {
      leftResult      <- normalizeMatch[F](
                           p.proc_1,
                           ProcVisitInputs(NilN, input.boundMapChain, FreeMap.empty),
                         )
      rightResult     <- normalizeMatch[F](
                           p.proc_2,
                           ProcVisitInputs(NilN, input.boundMapChain, FreeMap.empty),
                         )
      lp               = leftResult.par
      rp               = rightResult.par
      resultConnective = ConnOrN(Seq(lp, rp))

    } yield ProcVisitOutputs(
      ParN.combine(input.par, resultConnective),
      input.freeMap
        .addConnective(
          resultConnective,
          SourcePosition(p.line_num, p.col_num),
        ),
    )
}
