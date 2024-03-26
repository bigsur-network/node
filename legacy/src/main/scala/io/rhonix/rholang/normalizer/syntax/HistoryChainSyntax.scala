package io.rhonix.rholang.normalizer.syntax

import cats.effect.Sync
import cats.syntax.all.*
import coop.rchain.rholang.interpreter.compiler.{IdContext, SourcePosition}
import io.rhonix.rholang.normalizer.env.VarContext
import io.rhonix.rholang.normalizer.envimpl.{HistoryChain, VarMap}
import io.rhonix.rholang.normalizer.syntax.all.*

trait HistoryChainSyntax {
  implicit def normalizerSyntaxHistoryChain[T](c: HistoryChain[T]): HistoryChainOps[T] =
    new HistoryChainOps(c)

  implicit def normalizerSyntaxHistoryChainVarMap[T](c: HistoryChain[VarMap[T]]): HistoryChainVarMapOps[T] =
    new HistoryChainVarMapOps[T](c)
}

final class HistoryChainOps[T](private val chain: HistoryChain[T]) extends AnyVal {

  /**
   * Updates the current element in the HistoryChain.
   * @param f a transformation function that takes an element of type `T` and returns a transformed element of the same type
   */
  def updateCurrent(f: T => T): Unit = chain.push(f(chain.pop()))

  def modifyCurrent[R](f: T => (T, R)): R = {
    val (t, r) = f(chain.pop())
    chain.push(t)
    r
  }

  /** Run scopeFn with new data in the HistoryChain. */
  def runWithNewDataInChain[F[_]: Sync, R](scopeFn: F[R], newData: T): F[R] =
    for {
      _   <- Sync[F].delay(chain.push(newData))
      res <- scopeFn
      _    = chain.pop()
    } yield res
}

final class HistoryChainVarMapOps[T](private val chain: HistoryChain[VarMap[T]]) extends AnyVal {

  /**
   * Adds new variables to the current variable map and returns they indices.
   *
   * @param vars the data of variables to add.
   * @return indices of the added variables.
   */
  def putVars(vars: Seq[IdContext[T]]): Seq[VarContext[T]] =
    // TODO: Move multiple put to VarMap with returning indices in the same call.
    vars
      .map { case (name, sort, sourcePosition) =>
        chain.modifyCurrent(_.put(name, sort, sourcePosition))
      }

  /**
   * Creates new variables without names (internally increases next index).
   *
   * @param vars type of variables to add.
   * @return references of added variables.
   */
  def createBoundVars(vars: Seq[(T, SourcePosition)]): Seq[VarContext[T]] = chain.modifyCurrent(_.create(vars))

  /**
   * Searches for a variable in the chain of variable maps and returns the first match.
   *
   * @param name the name of the variable.
   * @return an option containing a tuple with the variable context if the variable exists, None otherwise.
   */
  def getFirstVarInChain(name: String): Option[VarContext[T]] =
    chain.iter.toSeq.collectFirstSome(_.get(name))
}
