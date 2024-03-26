package coop.rchain.rholang.interpreter.compiler.normalizer.processes

import cats.effect.Sync
import cats.syntax.all.*
import coop.rchain.models.Par
import io.rhonix.rholang.ast.rholang.Absyn.PCollect
import coop.rchain.rholang.interpreter.compiler.normalizer.CollectionNormalizeMatcher
import coop.rchain.rholang.interpreter.compiler.{CollectVisitInputs, ProcVisitInputs, ProcVisitOutputs}
import io.rhonix.rholang.types.ParN

object PCollectNormalizer {
  def normalize[F[_]: Sync](p: PCollect, input: ProcVisitInputs)(implicit
    env: Map[String, Par],
  ): F[ProcVisitOutputs] =
    CollectionNormalizeMatcher
      .normalizeMatch[F](p.collection_, CollectVisitInputs(input.boundMapChain, input.freeMap))
      .map { collectResult =>
        val expr = collectResult.expr
        ProcVisitOutputs(ParN.combine(input.par, expr), collectResult.freeMap)
      }
}
