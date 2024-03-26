package io.rhonix.rholang.normalizer.envimpl

import coop.rchain.rholang.interpreter.compiler.IdContext
import io.rhonix.rholang.normalizer.env.{FreeVarWriter, VarContext}
import io.rhonix.rholang.normalizer.syntax.all.*

final case class FreeVarWriterImpl[T](chain: HistoryChain[VarMap[T]]) extends FreeVarWriter[T] {
  override def putFreeVars(binding: Seq[IdContext[T]]): Seq[VarContext[T]] = chain.putVars(binding)
}
