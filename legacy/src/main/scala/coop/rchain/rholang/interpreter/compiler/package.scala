package coop.rchain.rholang.interpreter

package object compiler {

  /** A tuple of a variable name, its type and source position. */
  type IdContext[T] = (String, T, SourcePosition)
}
