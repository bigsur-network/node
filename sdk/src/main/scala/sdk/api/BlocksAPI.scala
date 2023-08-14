package sdk.api

trait BlocksAPI[Id, Block] {
  def find[T](id: Id, proj: Block => T): Option[T]

}

object BlocksAPI {
  final case class Block[BlockId, SenderID, TxId, StateId](
    id: BlockId,
    sender: SenderID,
    minGenJs: Set[BlockId],
    offences: Set[BlockId],
    txs: Set[TxId],
    state: StateId,
    fringe: Set[BlockId],
    finallyAccepted: Set[TxId],
    finallyRejected: Set[TxId],
  )
}
