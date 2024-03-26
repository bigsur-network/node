package io.rhonix.rholang.normalizer.util

import coop.rchain.rholang.interpreter.compiler.IdContext
import io.rhonix.rholang.normalizer.env.{FreeVarWriter, VarContext}
import io.rhonix.rholang.normalizer.util.Mock.{DefFreeVarIndex, FreeVarWriterData}

import scala.collection.mutable.ListBuffer

case class MockFreeVarWriter[F[_], T](scope: MockFreeVarScope[F]) extends FreeVarWriter[T] {
  private val buffer: ListBuffer[FreeVarWriterData[T]] = ListBuffer.empty

  override def putFreeVars(binding: Seq[IdContext[T]]): Seq[VarContext[T]] =
    binding.zipWithIndex.map { case ((name, varType, pos), i) =>
      val scopeLevel = scope.getScopeLevel
      buffer.append(FreeVarWriterData(name, varType, scopeLevel))
      VarContext(DefFreeVarIndex + i, -1, varType, pos)
    }

  def extractData: Seq[FreeVarWriterData[T]] = buffer.toSeq
}
