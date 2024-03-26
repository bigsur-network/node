package io.rhonix.rholang.normalizer

import cats.effect.Sync
import coop.rchain.rholang.interpreter.compiler.VarSort
import io.rhonix.rholang.ast.rholang.Absyn.*
import io.rhonix.rholang.normalizer.syntax.all.*
import io.rhonix.rholang.types.*

object Normalizer {

  /**
   * Flag to determine if normalizer will use reverse index when creating [[io.rhonix.rholang.types.BoundVarN]] term.
   *
   * NOTE: This flag exists to track where reverse index is used.
   *
   * TODO: Utilized get methods with index inversion as it functions equivalently to the legacy implementation.
   *       This approach ensures compatibility with the legacy reducer and associated tests.
   *       However, it should be rewritten using non-inverted methods following the completion of reducer rewriting.
   */
  val BOUND_VAR_INDEX_REVERSED = true

  /** Normalizes parser AST types to core Rholang AST types. Entry point of the normalizer.
   *
   * @param proc input parser AST object.
   * @return core Rholang AST object [[ParN]].
   */
  def normalize[F[_]: Sync](proc: Proc): F[ParN] = {
    val norm = new NormalizerRecImpl[F, VarSort]
    import norm.*
    norm.normalize(proc).withNewVarScope
  }
}
