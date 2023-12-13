package sdk.consensus

/**
 * Type class for quorum selection.
 * 
 * Fringe advancement can be blocked if fringe is not "all across" which means protocol is not live.
 *
 * This type class is to select the quorum of validators that have to be included in the next fringe
 * from the data type representing the latest fringe.
 *
 * Since the latest fringe is justified by set of messages on top (already produced), it gives attacker no
 * room for manipulation (DDoS), if the next fringe senders are derived from the current one.
 * */
trait SelectQuorum[A, B] {
  def next(x: A): Set[B]
}
