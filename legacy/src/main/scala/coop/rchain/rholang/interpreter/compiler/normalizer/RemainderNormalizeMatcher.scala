package coop.rchain.rholang.interpreter.compiler.normalizer

import cats.effect.Sync
import cats.syntax.all.*
import io.rhonix.rholang.ast.rholang.Absyn.*
import coop.rchain.rholang.interpreter.compiler.*
import coop.rchain.rholang.interpreter.errors.UnexpectedReuseOfProcContextFree
import io.rhonix.rholang.types.{FreeVarN, VarN, WildcardN}

object RemainderNormalizeMatcher {
  def handleProcVar[F[_]: Sync](
    pv: ProcVar,
    knownFree: FreeMap[VarSort],
  ): F[(Option[VarN], FreeMap[VarSort])] =
    pv match {
      case pvw: ProcVarWildcard =>
        (
          Option(WildcardN: VarN),
          knownFree.addWildcard(SourcePosition(pvw.line_num, pvw.col_num)),
        ).pure[F]
      case pvv: ProcVarVar      =>
        val sourcePosition = SourcePosition(pvv.line_num, pvv.col_num)
        knownFree.get(pvv.var_) match {
          case None                                         =>
            val newBindingsPair = knownFree.put((pvv.var_, ProcSort, sourcePosition))
            (Option(FreeVarN(knownFree.nextLevel): VarN), newBindingsPair).pure[F]
          case Some(FreeContext(_, _, firstSourcePosition)) =>
            Sync[F].raiseError(
              UnexpectedReuseOfProcContextFree(pvv.var_, firstSourcePosition, sourcePosition),
            )
        }
    }

  def normalizeMatchProc[F[_]: Sync](
    r: ProcRemainder,
    knownFree: FreeMap[VarSort],
  ): F[(Option[VarN], FreeMap[VarSort])] =
    r match {
      case _: ProcRemainderEmpty => (None: Option[VarN], knownFree).pure[F]
      case pr: ProcRemainderVar  =>
        handleProcVar[F](pr.procvar_, knownFree)
    }

  def normalizeMatchName[F[_]: Sync](
    nr: NameRemainder,
    knownFree: FreeMap[VarSort],
  ): F[(Option[VarN], FreeMap[VarSort])] =
    nr match {
      case _: NameRemainderEmpty => (None: Option[VarN], knownFree).pure[F]
      case nr: NameRemainderVar  =>
        handleProcVar[F](nr.procvar_, knownFree)
    }
}
