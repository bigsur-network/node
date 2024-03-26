package io.rhonix.rholang.normalizer

import cats.effect.Sync
import cats.syntax.all.*
import coop.rchain.rholang.interpreter.compiler.*
import coop.rchain.rholang.interpreter.errors.UnrecognizedNormalizerError
import io.rhonix.rholang.ast.rholang.Absyn.*
import io.rhonix.rholang.normalizer.NormalizerRecImpl.normalizeRecInternal
import io.rhonix.rholang.normalizer.env.*
import io.rhonix.rholang.normalizer.envimpl.*
import io.rhonix.rholang.types.*
import sdk.syntax.all.*

final case class NormalizerRecImpl[F[_]: Sync, T >: VarSort]() extends NormalizerRec[F] {

  // Normalizer dependencies

  implicit val nRec: NormalizerRec[F] = this

  val boundMapChain: HistoryChain[VarMap[T]] = HistoryChain.default[VarMap[T]]

  implicit val boundVarWriter: BoundVarWriter[T] = BoundVarWriterImpl(boundMapChain)

  implicit val boundVarReader: BoundVarReader[T] = BoundVarReaderImpl(boundMapChain)

  implicit val boundVarScope: BoundVarScope[F] = BoundVarScopeImpl(boundMapChain)

  val freeMapChain: HistoryChain[VarMap[T]] = HistoryChain.default[VarMap[T]]

  implicit val freeVarWriter: FreeVarWriter[T] = FreeVarWriterImpl(freeMapChain)

  implicit val freeVarReader: FreeVarReader[T] = FreeVarReaderImpl(freeMapChain)

  implicit val freeVarScope: FreeVarScope[F] = FreeVarScopeImpl(freeMapChain)

  val patternInfoChain: HistoryChain[(Int, Boolean)] = HistoryChain.default[(Int, Boolean)]
  // TODO: Move initialization of internal state to functions on the interface.
  patternInfoChain.push((0, false))

  val bundleInfoChain: HistoryChain[Boolean] = HistoryChain.default[Boolean]
  // TODO: Move initialization of internal state to functions on the interface.
  bundleInfoChain.push(false)

  implicit val nestingInfoWriter: NestingWriter[F] = NestingWriterImpl(patternInfoChain, bundleInfoChain)
  implicit val nestingInfoReader: NestingReader    = NestingReaderImpl(patternInfoChain, bundleInfoChain)

  // Normalizer interface implementation / recursive normalizer functions

  override def normalize(proc: Proc): F[ParN] = Sync[F].defer(normalizeRecInternal[F, T](proc))

  override def normalize(name: Name): F[ParN] = Sync[F].defer(name match {
    case nv: NameVar      =>
      VarNormalizer.normalizeBoundVar[F, T](nv.var_, SourcePosition(nv.line_num, nv.col_num), NameSort).widen
    case nq: NameQuote    => NormalizerRec[F].normalize(nq.proc_)
    case wc: NameWildcard => VarNormalizer.normalizeWildcard[F](SourcePosition(wc.line_num, wc.col_num)).widen
  })

  override def normalize(remainder: ProcRemainder): F[Option[VarN]] = Sync[F].defer(remainder match {
    case _: ProcRemainderEmpty => Sync[F].pure(None)
    case pr: ProcRemainderVar  => VarNormalizer.normalizeRemainder[F, T](pr.procvar_).map(_.some)
  })

  override def normalize(remainder: NameRemainder): F[Option[VarN]] = Sync[F].defer(remainder match {
    case _: NameRemainderEmpty => Sync[F].pure(None)
    case nr: NameRemainderVar  => VarNormalizer.normalizeRemainder[F, T](nr.procvar_).map(_.some)
  })

  // TODO: Temporary initialization function to use in tests until complete solution is created.
  def init: this.type = {
    boundMapChain.push(VarMap.default[T])
    freeMapChain.push(VarMap.default[T])
    this
  }
}

object NormalizerRecImpl {

  private def normalizeRecInternal[
    F[_]: Sync: NormalizerRec: BoundVarScope: FreeVarScope: NestingWriter,
    T >: VarSort: BoundVarWriter: BoundVarReader: FreeVarWriter: FreeVarReader,
  ](proc: Proc)(implicit nestingInfo: NestingReader): F[ParN] = {

    def unaryExp(subProc: Proc, constructor: ParN => ExprN): F[ParN] =
      NormalizerRec[F].normalize(subProc).map(constructor)

    def binaryExp(subProcLeft: Proc, subProcRight: Proc, constructor: (ParN, ParN) => ExprN): F[ParN] =
      (subProcLeft, subProcRight).nmap(NormalizerRec[F].normalize).mapN(constructor)

    // Dispatch to normalizer methods depending on parser AST type
    proc match {
      /* Terminal expressions (0-arity constructors) */
      /* =========================================== */
      case _: PNil        => (NilN: ParN).pure
      case p: PGround     => GroundNormalizer.normalizeGround[F](p).widen
      case p: PVar        => VarNormalizer.normalizeVar[F, T](p).widen
      case p: PVarRef     => VarRefNormalizer.normalizeVarRef[F, T](p).widen
      case p: PSimpleType => Sync[F].delay(SimpleTypeNormalizer.normalizeSimpleType(p))

      /* Unary expressions (1-arity constructors) */
      /* ======================================== */
      case p: PBundle   => BundleNormalizer.normalizeBundle[F](p).widen
      case p: PNegation => NegationNormalizer.normalizeNegation[F](p).widen
      case p: PEval     => NormalizerRec[F].normalize(p.name_)
      case p: PExprs    => NormalizerRec[F].normalize(p.proc_)
      case p: PNot      => unaryExp(p.proc_, ENotN.apply)
      case p: PNeg      => unaryExp(p.proc_, ENegN.apply)

      /* Binary expressions (2-arity constructors) */
      /* ========================================= */
      case p: PPar            => ParNormalizer.normalizePar[F](p)
      case p: PMatches        => MatchesNormalizer.normalizeMatches[F](p).widen
      case p: PConjunction    => ConjunctionNormalizer.normalizeConjunction[F](p).widen
      case p: PDisjunction    => DisjunctionNormalizer.normalizeDisjunction[F](p).widen
      case p: PMult           => binaryExp(p.proc_1, p.proc_2, EMultN.apply)
      case p: PDiv            => binaryExp(p.proc_1, p.proc_2, EDivN.apply)
      case p: PMod            => binaryExp(p.proc_1, p.proc_2, EModN.apply)
      case p: PPercentPercent => binaryExp(p.proc_1, p.proc_2, EPercentPercentN.apply)
      case p: PAdd            => binaryExp(p.proc_1, p.proc_2, EPlusN.apply)
      case p: PMinus          => binaryExp(p.proc_1, p.proc_2, EMinusN.apply)
      case p: PPlusPlus       => binaryExp(p.proc_1, p.proc_2, EPlusPlusN.apply)
      case p: PMinusMinus     => binaryExp(p.proc_1, p.proc_2, EMinusMinusN.apply)
      case p: PLt             => binaryExp(p.proc_1, p.proc_2, ELtN.apply)
      case p: PLte            => binaryExp(p.proc_1, p.proc_2, ELteN.apply)
      case p: PGt             => binaryExp(p.proc_1, p.proc_2, EGtN.apply)
      case p: PGte            => binaryExp(p.proc_1, p.proc_2, EGteN.apply)
      case p: PEq             => binaryExp(p.proc_1, p.proc_2, EEqN.apply)
      case p: PNeq            => binaryExp(p.proc_1, p.proc_2, ENeqN.apply)
      case p: PAnd            => binaryExp(p.proc_1, p.proc_2, EAndN.apply)
      case p: POr             => binaryExp(p.proc_1, p.proc_2, EOrN.apply)
      case p: PShortAnd       => binaryExp(p.proc_1, p.proc_2, EShortAndN.apply)
      case p: PShortOr        => binaryExp(p.proc_1, p.proc_2, EShortOrN.apply)

      /* N-ary parameter expressions (N-arity constructors) */
      /* ================================================== */
      case p: PCollect   => CollectNormalizer.normalizeCollect[F](p).widen
      case p: PSend      => SendNormalizer.normalizeSend[F](p).widen
      case p: PSendSynch => SendSyncNormalizer.normalizeSendSync[F, T](p)
      case p: PContr     => ContractNormalizer.normalizeContract[F, T](p).widen
      case p: PInput     => InputNormalizer.normalizeInput(p)
      case p: PNew       => NewNormalizer.normalizeNew[F, T](p).widen
      case p: PLet       => LetNormalizer.normalizeLet[F, T](p)
      case p: PMatch     => MatchNormalizer.normalizeMatch[F, T](p).widen
      case p: PIf        => IfNormalizer.normalizeIf[F](p).widen
      case p: PIfElse    => IfNormalizer.normalizeIfElse[F](p).widen
      case p: PMethod    => MethodNormalizer.normalizeMethod[F](p).widen

      case p => UnrecognizedNormalizerError(s"Unrecognized parser AST type `${p.getClass}`.").raiseError
    }
  }
}
