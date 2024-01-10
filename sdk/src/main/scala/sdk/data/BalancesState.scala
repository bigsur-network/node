package sdk.data

import sdk.primitive.ByteArray

/**
 * Minimalistic state that support only storing balances.
 * Analogous to the full tuple space it can be represent with a map.
 * */
final case class BalancesState(diffs: Map[ByteArray, Long]) {
  // Replace or add records with data from the input balances
  def ++(that: BalancesState) = new BalancesState(this.diffs ++ that.diffs)

  override def toString: String = diffs.toString()
}

object BalancesState {
  val Default = new BalancesState(Map.empty[ByteArray, Long])
}
