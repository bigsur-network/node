package sdk.merging

import cats.effect.kernel.Sync
import cats.syntax.all.*
import sdk.data.{BalancesDeploy, BalancesState}
import sdk.diag.Metrics
import sdk.history.ByteArray32
import sdk.primitive.ByteArray
import sdk.syntax.all.{effectSyntax, mapSyntax}

object MergeLogicForPayments {

  /**
   * Compute final values for records changed by the deploy.
   *
   * @return State containing changed values.
   *         None if negative value or long overflow detected. In this case, deploy should be rejected.
   * */
  def attemptCombine(
    allBalances: BalancesState,
    deploy: BalancesDeploy,
  ): Option[BalancesState] =
    deploy.body.state.diffs.foldLeft(allBalances.some) { case (acc, (wallet, change)) =>
      acc match {
        case None      => acc
        case Some(acc) =>
          // Input args should ensure item is present in a map
          val curV = allBalances.diffs.getUnsafe(wallet)
          // Overflow should be fatal, since this is related to total supply
          val newV = Math.addExact(curV, change)
          Option.unless(newV < 0)(newV).as(new BalancesState(acc.diffs + (wallet -> newV)))
      }
    }

  /**
   * Fold a sequence of items into initial state. Combination of an item with the state can fail.
   *
   * @return new state and items that failed to be combined.
   * */
  def foldCollectFailures[A, B](z: A, x: Seq[B], attemptCombine: (A, B) => Option[A]): (A, Seq[B]) =
    x.foldLeft(z, Seq.empty[B]) { case ((acc, rjAcc), x) =>
      attemptCombine(acc, x).map(_ -> rjAcc).getOrElse(acc -> (x +: rjAcc))
    }

  /**
   * Merge deploys into the base state rejecting those leading to overflow.
   * */
  def mergeRejectNegativeOverflow[F[_]: Sync: Metrics](
    readBaseBalance: (ByteArray32, ByteArray) => F[Option[Long]],
    baseState: ByteArray32,
    toFinalize: Set[BalancesDeploy],
    toMerge: Set[BalancesDeploy],
  ): F[((BalancesState, Seq[BalancesDeploy]), (BalancesState, Seq[BalancesDeploy]))] = Sync[F].defer {
    val adjustedInFinal: Set[ByteArray] = toFinalize.flatMap(_.body.state.diffs.keys)
    val adjustedInMerge: Set[ByteArray] = toMerge.flatMap(_.body.state.diffs.keys)
    val adjustedAll: Set[ByteArray]     = adjustedInFinal ++ adjustedInMerge

    val readAllBalances = adjustedAll.toList
      .traverse(k => readBaseBalance(baseState, k).map(_.getOrElse(0L)).map(k -> _))
      .map(_.toMap)

    readAllBalances
      .flatMap { allInitValues =>
        val initFinal        = new BalancesState(allInitValues.view.filterKeys(adjustedInFinal.contains).toMap)
        val initAll          = new BalancesState(allInitValues)
        val toFinalizeSorted = toFinalize.toList.sorted
        val toMergeSorted    = toMerge.toList.sorted

        Sync[F]
          .delay(foldCollectFailures(initFinal, toFinalizeSorted, attemptCombine))
          .timedM("buildFinalState")
          .map { case (finChange, finRj) =>
            val (mergeChange, provRj) = foldCollectFailures(initAll ++ finChange, toMergeSorted, attemptCombine)
            (finChange, finRj) -> (mergeChange, provRj)
          }
      }
  }
}
