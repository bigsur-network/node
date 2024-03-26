package io.rhonix.rholang.normalizer.util

import coop.rchain.rholang.interpreter.compiler.FreeContext
import Mock.DefPosition
import io.rhonix.rholang.normalizer.env.FreeVarReader

case class MockFreeVarReader[T](
  freeVars: Map[String, (Int, T)],
) extends FreeVarReader[T] {

  private val freeVarMap: Map[String, FreeContext[T]] =
    freeVars.map { case (name, (index, varType)) => name -> FreeContext(index, varType, DefPosition) }

  override def getFreeVars: Seq[(String, FreeContext[T])]       = freeVarMap.toSeq
  override def getFreeVar(name: String): Option[FreeContext[T]] = freeVarMap.get(name)
}
