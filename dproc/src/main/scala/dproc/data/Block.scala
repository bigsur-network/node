package dproc.data

import sdk.consensus.data.BondsMap
import weaver.GardState.GardM
import weaver.data.*

/**
 * All data required to be packed in a block.
 * @param sender block sender
 * @param minGenJs minimal generative justifications set
 * @param offences offences computed by the message
 * @param txs transactions executed in a block
 * @param finalFringe final fringe computed by the message
 * @param finalized finalization computed by the message
 * @param merge rejections on conflict set merge computed by the message
 * @param bonds bonds map of a message
 * @param lazTol laziness tolerance
 * @param expThresh transaction expiration threshold
 * @tparam M type for block ID
 * @tparam S type for sender ID
 * @tparam T type for transaction ID
 */
final case class Block[M, S, T](
  sender: S,
  minGenJs: Set[M],
  offences: Set[M],
  txs: List[T],
  finalFringe: Set[M],
  finalized: Option[ConflictResolution[T]],
  merge: Set[T],
  bonds: BondsMap[S],
  lazTol: Int,
  expThresh: Int,
  finalStateHash: Array[Byte],
  postStateHash: Array[Byte],
)

object Block {
  final case class WithId[M, S, T](id: M, m: Block[M, S, T])

  def toLazoM[M, S, T](m: Block[M, S, T]): MessageData[M, S] = MessageData(
    m.sender,
    m.minGenJs,
    m.offences,
    FringeData(m.finalFringe),
    FinalData(m.bonds, m.lazTol, m.expThresh),
  )

  def toLazoE[M, S, T](m: Block[M, S, T]): FinalData[S] =
    FinalData(m.bonds, m.lazTol, m.expThresh)

  def toGardM[M, S, T](m: Block[M, S, T]): GardM[M, T] = GardM(m.txs.toSet, m.finalFringe)
}
