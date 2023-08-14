package weaver

import weaver.GardState.GardM
import weaver.data.*

/** The state of the process are state supporting all protocols that the process run. */
final case class WeaverState[M, S, T](
  lazo: LazoState[M, S],
  meld: MeldState[M, T],
  gard: GardState[M, T],
) {
  def add(
    id: M,
    lazoM: MessageData.Extended[M, S],
    meldMOpt: Option[MergingData[T]],
    gardMOpt: Option[GardM[M, T]],
    offenceOpt: Option[Offence],
  ): (WeaverState[M, S, T], (Set[M], Boolean)) =
    if (lazo.contains(id)) (this, Set.empty[M] -> false)
    else {
      val (newLazo, gc) = lazo.add(id, lazoM, offenceOpt)
      val newMeld       = meldMOpt.map(meld.add(id, _, gc)).getOrElse(meld)
      val newGard       = gardMOpt.map(gard.add).getOrElse(gard)
      val newSt         = WeaverState(newLazo, newMeld, newGard)
      (newSt, gc -> true)
    }
}

object WeaverState {
  def empty[M, S, T](trust: FinalData[S]): WeaverState[M, S, T] =
    WeaverState(LazoState.empty(trust), MeldState.empty, GardState.empty)

  def shouldAdd[M, S](lazo: LazoState[M, S], msgId: M, minGenJs: Set[M], sender: S): Boolean =
    // if already added - ignore
    if (lazo.contains(msgId)) false
    // or check with Lazo rules
    else LazoState.canAdd(minGenJs, sender, lazo)
}
