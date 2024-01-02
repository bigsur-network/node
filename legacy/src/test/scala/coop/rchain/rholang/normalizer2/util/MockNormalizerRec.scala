package coop.rchain.rholang.normalizer2.util

import cats.Applicative
import cats.implicits.{catsSyntaxApplicativeId, none}
import coop.rchain.rholang.normalizer2.NormalizerRec
import coop.rchain.rholang.normalizer2.util.Mock.*
import coop.rchain.rholang.normalizer2.util.MockNormalizerRec.{mockADT, RemainderADTDefault}
import io.rhonix.rholang.{GStringN, NilN, ParN, VarN}
import io.rhonix.rholang.ast.rholang.Absyn.{GroundString, Name, NameRemainder, PGround, Proc, ProcRemainder}

import scala.collection.mutable.ListBuffer

case class MockNormalizerRec[F[_]: Applicative, T](mockBVW: MockBoundVarWriter[T], mockFVW: MockFreeVarWriter[T])
    extends NormalizerRec[F] {
  private val buffer: ListBuffer[TermData] = ListBuffer.empty

  private def addInBuf(term: MockNormalizerRecTerm): Unit =
    buffer.append(
      TermData(
        term = term,
        boundNewScopeLevel = mockBVW.getNewScopeLevel,
        boundCopyScopeLevel = mockBVW.getCopyScopeLevel,
        freeScopeLevel = mockFVW.getScopeLevel,
      ),
    )

  override def normalize(proc: Proc): F[ParN] = {
    addInBuf(ProcTerm(proc))
    mockADT(proc).pure
  }

  override def normalize(name: Name): F[ParN] = {
    addInBuf(NameTerm(name))
    mockADT(name).pure
  }

  override def normalize(remainder: ProcRemainder): F[Option[VarN]] = {
    addInBuf(ProcRemainderTerm(remainder))
    RemainderADTDefault.pure
  }

  override def normalize(remainder: NameRemainder): F[Option[VarN]] = {
    addInBuf(NameRemainderTerm(remainder))
    RemainderADTDefault.pure
  }

  def extractData: Seq[TermData] = buffer.toSeq

}

object MockNormalizerRec {
  def mockADT(proc: Proc): ParN         = GStringN(proc.toString)
  def mockADT(name: Name): ParN         = GStringN(name.toString)
  val RemainderADTDefault: Option[VarN] = none
}