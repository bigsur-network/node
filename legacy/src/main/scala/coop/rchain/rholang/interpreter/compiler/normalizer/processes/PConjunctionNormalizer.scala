package coop.rchain.rholang.interpreter.compiler.normalizer.processes

import cats.effect.Sync
import cats.syntax.all.*
import coop.rchain.models.Par
import io.rhonix.rholang.ast.rholang.Absyn.PConjunction
import coop.rchain.rholang.interpreter.compiler.ProcNormalizeMatcher.normalizeMatch
import coop.rchain.rholang.interpreter.compiler.{ProcVisitInputs, ProcVisitOutputs, SourcePosition}
import io.rhonix.rholang.types.{ConnAndN, NilN, ParN}

object PConjunctionNormalizer {
  def normalize[F[_]: Sync](p: PConjunction, input: ProcVisitInputs)(implicit
    env: Map[String, Par],
  ): F[ProcVisitOutputs] =
    for {
      leftResult  <- normalizeMatch[F](
                       p.proc_1,
                       ProcVisitInputs(NilN, input.boundMapChain, input.freeMap),
                     )
      rightResult <- normalizeMatch[F](
                       p.proc_2,
                       ProcVisitInputs(NilN, input.boundMapChain, leftResult.freeMap),
                     )
      lp           = leftResult.par
      rp           = rightResult.par

      resultConnective = ConnAndN(Seq(lp, rp))

    } yield ProcVisitOutputs(
      ParN.combine(input.par, resultConnective),
      rightResult.freeMap
        .addConnective(
          resultConnective,
          SourcePosition(p.line_num, p.col_num),
        ),
    )
}
