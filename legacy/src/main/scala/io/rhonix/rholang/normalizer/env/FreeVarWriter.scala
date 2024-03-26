package io.rhonix.rholang.normalizer.env

import coop.rchain.rholang.interpreter.compiler.IdContext

trait FreeVarWriter[T] {

  /**
   * Add free variables.
   *
   * @return De Bruijn index of the variable added.
   * */
  def putFreeVars(binding: Seq[IdContext[T]]): Seq[VarContext[T]]
}

object FreeVarWriter {
  def apply[T](implicit instance: FreeVarWriter[T]): FreeVarWriter[T] = instance
}
