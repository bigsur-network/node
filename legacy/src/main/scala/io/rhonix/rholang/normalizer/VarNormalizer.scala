package io.rhonix.rholang.normalizer

import cats.effect.Sync
import cats.syntax.all.*
import coop.rchain.rholang.interpreter.compiler.*
import coop.rchain.rholang.interpreter.errors.*
import io.rhonix.rholang.ast.rholang.Absyn.*
import io.rhonix.rholang.normalizer.Normalizer.BOUND_VAR_INDEX_REVERSED
import io.rhonix.rholang.normalizer.env.*
import io.rhonix.rholang.normalizer.syntax.all.*
import io.rhonix.rholang.types.{BoundVarN, FreeVarN, VarN, WildcardN}

object VarNormalizer {
  def normalizeVar[F[_]: Sync, T >: VarSort: BoundVarReader: FreeVarReader: FreeVarWriter](
    p: PVar,
  )(implicit nestingInfo: NestingReader): F[VarN] = {
    def pos = SourcePosition(p.line_num, p.col_num)
    p.procvar_ match {
      case pvv: ProcVarVar    => normalizeBoundVar[F, T](pvv.var_, pos, ProcSort)
      case _: ProcVarWildcard => normalizeWildcard[F](pos)
    }
  }

  def normalizeRemainder[F[_]: Sync, T >: VarSort: FreeVarReader: FreeVarWriter](
    pv: ProcVar,
  )(implicit nestingInfo: NestingReader): F[VarN] =
    pv match {
      case pvv: ProcVarVar      => normalizeFreeVar[F, T](pvv.var_, SourcePosition(pvv.line_num, pvv.col_num), ProcSort)
      case pvw: ProcVarWildcard => normalizeWildcard[F](SourcePosition(pvw.line_num, pvw.col_num))
    }

  def normalizeBoundVar[F[_]: Sync, T: BoundVarReader: FreeVarReader: FreeVarWriter](
    varName: String,
    pos: SourcePosition,
    expectedSort: T,
  )(implicit nestingInfo: NestingReader): F[VarN] = Sync[F].defer {
    BoundVarReader[T].getBoundVar(varName) match {
      case Some(VarContext(index, indexFromEnd, `expectedSort`, _)) =>
        Sync[F].pure {
          // NOTE: Gets the index from latest bound variable. This is what reducer expects because every evaluation of a term
          //       creates new Env with starting index 0.
          // https://github.com/tgrospic/RhonixNode/blob/20ce195ad4/legacy/src/main/scala/coop/rchain/rholang/interpreter/dispatch.scala#L31
          val idx = if (BOUND_VAR_INDEX_REVERSED) indexFromEnd else index
          BoundVarN(idx)
        }
      case Some(VarContext(_, _, _, sourcePosition))                =>
        expectedSort match {
          case ProcSort => UnexpectedProcContext(varName, sourcePosition, pos).raiseError
          case NameSort => UnexpectedNameContext(varName, sourcePosition, pos).raiseError
        }

      case None => normalizeFreeVar[F, T](varName, pos, expectedSort)
    }
  }

  private def normalizeFreeVar[F[_]: Sync, T: FreeVarReader: FreeVarWriter](
    varName: String,
    pos: SourcePosition,
    expectedSort: T,
  )(implicit nestingInfo: NestingReader): F[VarN] =
    Sync[F].defer {
      if (nestingInfo.patternDepth > 0) { // If we inside a pattern
        // Inside bundle target is prohibited to have free variables.
        if (nestingInfo.insideBundle) UnexpectedBundleContent(s"Illegal free variable in bundle at $pos").raiseError
        else
          FreeVarReader[T].getFreeVar(varName) match {
            case None =>
              val freeVar = FreeVarWriter[T].putFreeVar(varName, expectedSort, pos)
              Sync[F].pure(FreeVarN(freeVar.index))

            case Some(FreeContext(_, _, firstSourcePosition)) =>
              expectedSort match {
                case ProcSort => UnexpectedReuseOfProcContextFree(varName, firstSourcePosition, pos).raiseError
                case NameSort => UnexpectedReuseOfNameContextFree(varName, firstSourcePosition, pos).raiseError
              }
          }
      } else TopLevelFreeVariablesNotAllowedError(s"$varName at $pos").raiseError
    }

  def normalizeWildcard[F[_]: Sync](pos: SourcePosition)(implicit nestingInfo: NestingReader): F[VarN] =
    if (nestingInfo.patternDepth > 0) // If we inside a pattern
      if (!nestingInfo.insideBundle) Sync[F].pure(WildcardN)
      // Inside bundle target is prohibited to have wildcards.
      else UnexpectedBundleContent(s"Illegal wildcard in bundle at $pos").raiseError
    else TopLevelWildcardsNotAllowedError(s"_ (wildcard) at $pos").raiseError

}
