package weaver.data

import sdk.consensus.data.BondsMap

/**
  * Data from execution engine. Everything that matters for consensus but can be adjusted programmatically.
  * State that really matters is a final state, so this data is read from the state of the final fringe
  * computed by the message.
  */
final case class FinalData[S](
  bonds: BondsMap[S],
  lazinessTolerance: Int,
  expirationThreshold: Int,
)
