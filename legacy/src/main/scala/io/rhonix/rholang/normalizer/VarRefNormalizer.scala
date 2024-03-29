package io.rhonix.rholang.normalizer

import cats.effect.Sync
import cats.syntax.all.*
import coop.rchain.rholang.interpreter.compiler.*
import coop.rchain.rholang.interpreter.errors.*
import io.rhonix.rholang.ast.rholang.Absyn.*
import io.rhonix.rholang.normalizer.env.*
import io.rhonix.rholang.types.ConnVarRefN

object VarRefNormalizer {
  def normalizeVarRef[F[_]: Sync, T >: VarSort: BoundVarReader](
    p: PVarRef,
  )(implicit nestingInfo: NestingReader): F[ConnVarRefN] =
    Sync[F].delay(BoundVarReader[T].findBoundVar(p.var_)).flatMap {
      // Found bounded variable
      case Some(VarContext(index, _, kind, sourcePosition)) =>
        val depth = nestingInfo.patternDepth
        // TODO: Throw an exception if VarRef is outside the pattern?
        kind match {
          case ProcSort =>
            p.varrefkind_ match {
              case _: VarRefKindProc => ConnVarRefN(index, depth).pure
              case _                 => UnexpectedProcContext(p.var_, sourcePosition, SourcePosition(p.line_num, p.col_num)).raiseError
            }
          case NameSort =>
            p.varrefkind_ match {
              case _: VarRefKindName => ConnVarRefN(index, depth).pure
              case _                 => UnexpectedNameContext(p.var_, sourcePosition, SourcePosition(p.line_num, p.col_num)).raiseError
            }
        }

      // Bounded variable not found
      case None => UnboundVariableRef(p.var_, p.line_num, p.col_num).raiseError
    }
}
