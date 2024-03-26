package io.rhonix.rholang.normalizer.envimpl

import cats.effect.kernel.Sync
import io.rhonix.rholang.normalizer.env.BoundVarScope
import io.rhonix.rholang.normalizer.syntax.all.*

final case class BoundVarScopeImpl[F[_]: Sync, T](chain: HistoryChain[VarMap[T]]) extends BoundVarScope[F] {
  override def withNewBoundVarScope[R](scopeFn: F[R]): F[R]  = chain.runWithNewDataInChain(scopeFn, VarMap.default[T])
  override def withCopyBoundVarScope[R](scopeFn: F[R]): F[R] = chain.runWithNewDataInChain(scopeFn, chain.current())
}
