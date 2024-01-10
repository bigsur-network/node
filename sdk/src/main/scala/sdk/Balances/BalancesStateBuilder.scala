package sdk.Balances

import sdk.data.BalancesState
import sdk.history.ByteArray32

trait BalancesStateBuilder {

  /**
   * Build final and post state.
   * @param baseState hash of the base state
   * @param toFinalize diff to finalize
   * @param toMerge diff to merge
   * @return hash of the final state and post state
   */
  def buildState[F[_]](
    baseState: ByteArray32,
    toFinalize: BalancesState,
    // Since execution is not different from merging there is no execution input
    toMerge: BalancesState,
  ): F[(ByteArray32, ByteArray32)]
}
