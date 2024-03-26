package coop.rchain.rholang.interpreter.compiler.normalizer.processes

import cats.effect.Sync
import cats.syntax.all.*
import io.rhonix.rholang.ast.rholang.Absyn.{PVarRef, VarRefKindName, VarRefKindProc}
import coop.rchain.rholang.interpreter.compiler.*
import coop.rchain.rholang.interpreter.errors.{UnboundVariableRef, UnexpectedNameContext, UnexpectedProcContext}
import io.rhonix.rholang.types.{ConnVarRefN, ParN}

object PVarRefNormalizer {
  def normalize[F[_]: Sync](p: PVarRef, input: ProcVisitInputs): F[ProcVisitOutputs] =
    input.boundMapChain.find(p.var_) match {
      case None                                                   =>
        Sync[F].raiseError(UnboundVariableRef(p.var_, p.line_num, p.col_num))
      case Some((BoundContext(idx, kind, sourcePosition), depth)) =>
        kind match {
          case ProcSort =>
            p.varrefkind_ match {
              case _: VarRefKindProc =>
                ProcVisitOutputs(ParN.combine(input.par, ConnVarRefN(idx, depth)), input.freeMap)
                  .pure[F]
              case _                 =>
                Sync[F].raiseError(
                  UnexpectedProcContext(
                    p.var_,
                    sourcePosition,
                    SourcePosition(p.line_num, p.col_num),
                  ),
                )
            }
          case NameSort =>
            p.varrefkind_ match {
              case _: VarRefKindName =>
                ProcVisitOutputs(ParN.combine(input.par, ConnVarRefN(idx, depth)), input.freeMap)
                  .pure[F]
              case _                 =>
                Sync[F].raiseError(
                  UnexpectedNameContext(
                    p.var_,
                    sourcePosition,
                    SourcePosition(p.line_num, p.col_num),
                  ),
                )
            }
        }
    }
}
