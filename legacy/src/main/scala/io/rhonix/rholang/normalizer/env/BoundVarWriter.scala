package io.rhonix.rholang.normalizer.env

import coop.rchain.rholang.interpreter.compiler.{IdContext, SourcePosition}

trait BoundVarWriter[T] {

  /**
   * Add bound variables.
   *
   * @param bindings sequence of [[IdContext]].
   * @return bound variables in bounds map that result in application of bindings.
   */
  def putBoundVars(bindings: Seq[IdContext[T]]): Seq[VarContext[T]]

  /**
   * Creates a new bound variable without user level (textual) name.
   *
   * @param vars data for variables to create.
   * @return indices of added bound variables.
   */
  def createBoundVars(vars: Seq[(T, SourcePosition)]): Seq[VarContext[T]]
}

object BoundVarWriter {
  def apply[T](implicit instance: BoundVarWriter[T]): BoundVarWriter[T] = instance
}
