package io.rhonix.rholang.normalizer.util

import cats.effect.Sync
import cats.syntax.all.*
import io.rhonix.rholang.normalizer.env.BoundVarScope

case class MockBoundVarScope[F[_]: Sync]() extends BoundVarScope[F] {
  private var newScopeLevel: Int  = 0
  private var copyScopeLevel: Int = 0

  override def withNewBoundVarScope[R](scopeFn: F[R]): F[R] = for {
    _   <- Sync[F].delay(newScopeLevel += 1)
    res <- scopeFn
    _    = newScopeLevel -= 1
  } yield res

  override def withCopyBoundVarScope[R](scopeFn: F[R]): F[R] = for {
    _   <- Sync[F].delay(copyScopeLevel += 1)
    res <- scopeFn
    _    = copyScopeLevel -= 1
  } yield res

  def getNewScopeLevel: Int  = newScopeLevel
  def getCopyScopeLevel: Int = copyScopeLevel
}
