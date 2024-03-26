package io.rhonix.rholang.normalizer.syntax

import coop.rchain.rholang.interpreter.compiler.IdContext
import io.rhonix.rholang.normalizer.env.{FreeVarWriter, VarContext}

trait FreeVarWriterSyntax {
  implicit def normalizerSyntaxFreeVarWriter[T](fw: FreeVarWriter[T]): FreeVarWriterOps[T] = new FreeVarWriterOps(fw)
}

final class FreeVarWriterOps[T](private val fw: FreeVarWriter[T]) extends AnyVal {

  /**
   * Adds new variables to the current variable map and returns they indices.
   *
   * @param v the data of the variable to add.
   * @return index of the added variable.
   */
  def putFreeVar(v: IdContext[T]): VarContext[T] = fw.putFreeVars(Seq(v)).head
}
