package io.rhonix.rholang.normalizer.env

import coop.rchain.rholang.interpreter.compiler.SourcePosition

/**
 * Context of a variable (free or bound).
 *
 * @param index index in a free or bound map.
 * @param indexRev reversed index bottom-up from the latest index.
 * @param typ type of the variable.
 * @param sourcePosition source position of the variable.
 * @tparam T type of the variable.
 */
final case class VarContext[T](index: Int, indexRev: Int, typ: T, sourcePosition: SourcePosition)
