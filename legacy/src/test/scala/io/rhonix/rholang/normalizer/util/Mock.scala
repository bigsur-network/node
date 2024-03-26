package io.rhonix.rholang.normalizer.util

import cats.effect.Sync
import coop.rchain.rholang.interpreter.compiler.SourcePosition
import coop.rchain.rholang.normalizer2.util.{MockNestingReader, MockNestingWriter}
import io.rhonix.rholang.ast.rholang.Absyn.{Name, NameRemainder, Proc, ProcRemainder}

object Mock {

  case class BoundVarWriterData[T](name: String, varType: T, newScopeLevel: Int = 0, copyScopeLevel: Int = 0)
  case class FreeVarWriterData[T](name: String, varType: T, scopeLevel: Int = 0)
  case class VarReaderData[T](name: String, index: Int, typ: T)
  case class TermData(
    term: MockNormalizerRecTerm,
    boundNewScopeLevel: Int = 0,
    boundCopyScopeLevel: Int = 0,
    freeScopeLevel: Int = 0,
    insidePattern: Boolean = false,
    insideTopLevelReceive: Boolean = false,
    insideBundle: Boolean = false,
  )

  sealed trait MockNormalizerRecTerm
  final case class ProcTerm(proc: Proc)                            extends MockNormalizerRecTerm
  final case class NameTerm(name: Name)                            extends MockNormalizerRecTerm
  final case class ProcRemainderTerm(remainder: ProcRemainder)     extends MockNormalizerRecTerm
  final case class NameRemainderTerm(nameRemainder: NameRemainder) extends MockNormalizerRecTerm

  val DefPosition: SourcePosition = SourcePosition(0, 0)
  val DefFreeVarIndex: Int        = 0

  // (nRec, bVScope, bVW, bVR, fVScope, fVW, fVR, fVScopeReader)

  /**
   * Create a mock DSL for testing the normalizer.
   * @param initBoundVars initial bound variables for the mock bound variable reader.
   *                      The key is the name of the variable. The value is a tuple of the index and the type.
   * @param initFreeVars initial free variables for the mock free variable reader.
   *                      The key is the name of the variable. The value is a tuple of the index and the type.
   * @param isPattern initial value for the inside pattern flag of the mock restrict reader.
   * @param isReceivePattern initial value for the isReceivePattern flag of the mock restrict reader.
   * @param isBundle initial value for the isBundle flag of the mock restrict reader.
   * @return all the components of the mock DSL.
   */
  def createMockDSL[F[_]: Sync, T](
    initBoundVars: Map[String, (Int, T)] = Map[String, (Int, T)](),
    initFreeVars: Map[String, (Int, T)] = Map[String, (Int, T)](),
    patternDepth: Int = 0,
    isReceivePattern: Boolean = false,
    isBundle: Boolean = false,
  ): (
    MockNormalizerRec[F, T],
    MockBoundVarScope[F],
    MockBoundVarWriter[F, T],
    MockBoundVarReader[T],
    MockFreeVarScope[F],
    MockFreeVarWriter[F, T],
    MockFreeVarReader[T],
    MockNestingWriter[F],
    MockNestingReader,
  ) = {
    val mockBVScope: MockBoundVarScope[F] = MockBoundVarScope[F]()
    val mockBVW: MockBoundVarWriter[F, T] = MockBoundVarWriter[F, T](mockBVScope)
    val mockBVR: MockBoundVarReader[T]    = MockBoundVarReader[T](initBoundVars)

    val mockFVScope: MockFreeVarScope[F] = MockFreeVarScope[F]()
    val mockFVW: MockFreeVarWriter[F, T] = MockFreeVarWriter[F, T](mockFVScope)
    val mockFVR: MockFreeVarReader[T]    = MockFreeVarReader[T](initFreeVars)

    val mockIW: MockNestingWriter[F] = MockNestingWriter[F]()

    val mockIR: MockNestingReader = MockNestingReader(patternDepth, isReceivePattern, isBundle)

    val mockNormalizerRec: MockNormalizerRec[F, T] = MockNormalizerRec[F, T](mockBVScope, mockFVScope, mockIW, mockIR)
    (mockNormalizerRec, mockBVScope, mockBVW, mockBVR, mockFVScope, mockFVW, mockFVR, mockIW, mockIR)
  }
}
