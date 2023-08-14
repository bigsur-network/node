package sdk.consensus

/**
 * Data required for consensus but can be adjusted programmatically.
 */
trait ConsensusConfig[F[_], T, S] {
  def bonds(x: T): F[Map[S, Long]]
  def lazinessTolerance(x: T): F[Int]
  def expirationThreshold(x: T): F[Int]
}
