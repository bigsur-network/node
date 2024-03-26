package io.rhonix.rholang.normalizer.env

import coop.rchain.rholang.interpreter.compiler.FreeContext

trait FreeVarReader[T] {

  /**
   * Get all free variables.
   *
   * @return sequence of free variables with theirs names.
   */
  def getFreeVars: Seq[(String, FreeContext[T])]

  /** Get free variable by name. */
  def getFreeVar(name: String): Option[FreeContext[T]]
}

object FreeVarReader {
  def apply[T](implicit instance: FreeVarReader[T]): FreeVarReader[T] = instance
}
