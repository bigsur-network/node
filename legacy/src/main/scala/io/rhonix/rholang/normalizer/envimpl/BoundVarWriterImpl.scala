package io.rhonix.rholang.normalizer.envimpl

import coop.rchain.rholang.interpreter.compiler.{IdContext, SourcePosition}
import io.rhonix.rholang.normalizer.env.{BoundVarWriter, VarContext}
import io.rhonix.rholang.normalizer.syntax.all.*

final case class BoundVarWriterImpl[T](chain: HistoryChain[VarMap[T]]) extends BoundVarWriter[T] {
  override def putBoundVars(bindings: Seq[IdContext[T]]): Seq[VarContext[T]]       = chain.putVars(bindings)
  override def createBoundVars(vars: Seq[(T, SourcePosition)]): Seq[VarContext[T]] = chain.createBoundVars(vars)
}
