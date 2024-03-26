package io.rhonix.rholang.normalizer.util

import coop.rchain.rholang.interpreter.compiler.{IdContext, SourcePosition}
import io.rhonix.rholang.normalizer.env.{BoundVarWriter, VarContext}
import io.rhonix.rholang.normalizer.util.Mock.BoundVarWriterData

import scala.collection.mutable.ListBuffer

case class MockBoundVarWriter[F[_], T](scope: MockBoundVarScope[F]) extends BoundVarWriter[T] {
  private val buffer: ListBuffer[BoundVarWriterData[T]] = ListBuffer.empty

  override def putBoundVars(bindings: Seq[IdContext[T]]): Seq[VarContext[T]] = {
    val newScopeLevel  = scope.getNewScopeLevel
    val copyScopeLevel = scope.getCopyScopeLevel
    buffer.appendAll(bindings.map { case (name, varType, _) =>
      BoundVarWriterData(name, varType, newScopeLevel, copyScopeLevel)
    })
    bindings.zipWithIndex.map { case ((_, t, pos), i) => VarContext(i, -1, t, pos) }
  }

  def extractData: Seq[BoundVarWriterData[T]] = buffer.toSeq

  override def createBoundVars(vars: Seq[(T, SourcePosition)]): Seq[VarContext[T]] =
    // TODO: support bound variables without textual name
    Seq()
}
