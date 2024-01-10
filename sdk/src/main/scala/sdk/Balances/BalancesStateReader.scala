package sdk.Balances

import sdk.history.ByteArray32
import sdk.primitive.ByteArray

trait BalancesStateReader {

  /** Read balance of a wallet at particular state. */
  def readBalance[F[_]](state: ByteArray32, wallet: ByteArray): F[Option[Long]]
}
