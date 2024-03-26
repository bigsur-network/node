package coop.rchain.rholang.interpreter.compiler.normalizer.processes

import cats.effect.Sync
import cats.syntax.all.*
import coop.rchain.models.Par
import io.rhonix.rholang.ast.rholang.Absyn.Proc
import coop.rchain.rholang.interpreter.compiler.ProcNormalizeMatcher.normalizeMatch
import coop.rchain.rholang.interpreter.compiler.{ProcVisitInputs, ProcVisitOutputs}
import io.rhonix.rholang.types.{GBoolN, MatchCaseN, MatchN, NilN, ParN}

object PIfNormalizer {
  def normalize[F[_]: Sync](
    valueProc: Proc,
    trueBodyProc: Proc,
    falseBodyProc: Proc,
    input: ProcVisitInputs,
  )(implicit
    env: Map[String, Par],
  ): F[ProcVisitOutputs] =
    for {
      targetResult  <- normalizeMatch[F](valueProc, input)
      trueCaseBody  <- normalizeMatch[F](
                         trueBodyProc,
                         ProcVisitInputs(NilN, input.boundMapChain, targetResult.freeMap),
                       )
      falseCaseBody <- normalizeMatch[F](
                         falseBodyProc,
                         ProcVisitInputs(NilN, input.boundMapChain, trueCaseBody.freeMap),
                       )
      desugaredIf    = MatchN(
                         targetResult.par,
                         Seq(
                           MatchCaseN(GBoolN(true), trueCaseBody.par),
                           MatchCaseN(GBoolN(false), falseCaseBody.par),
                         ),
                       )
    } yield ProcVisitOutputs(ParN.combine(input.par, desugaredIf), falseCaseBody.freeMap)

}
