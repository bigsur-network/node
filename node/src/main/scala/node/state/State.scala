package node.state

import sdk.DagCausalQueue
import sdk.data.BalancesDeploy
import sdk.history.ByteArray32
import sdk.node.{Processor, Proposer}
import sdk.primitive.ByteArray
import weaver.WeaverState

final case class State(
  weaver: WeaverState[ByteArray, ByteArray, BalancesDeploy],
  proposer: Proposer.ST,
  processor: Processor.ST[ByteArray],
  buffer: DagCausalQueue[ByteArray],
  lfsHash: ByteArray32,
)
