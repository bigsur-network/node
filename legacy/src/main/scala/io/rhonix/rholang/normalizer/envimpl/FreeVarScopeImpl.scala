package io.rhonix.rholang.normalizer.envimpl

import cats.effect.kernel.Sync
import io.rhonix.rholang.normalizer.env.FreeVarScope
import io.rhonix.rholang.normalizer.syntax.all.*

final case class FreeVarScopeImpl[F[_]: Sync, T](chain: HistoryChain[VarMap[T]]) extends FreeVarScope[F] {
  override def withNewFreeVarScope[R](scopeFn: F[R]): F[R] = chain.runWithNewDataInChain(scopeFn, VarMap.default[T])
}
