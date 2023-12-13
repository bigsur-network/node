package weaver.syntax

import sdk.consensus.data.BondsMap
import weaver.LazoState
import weaver.data.MessageData
import weaver.data.MessageData.Extra
import weaver.syntax.all.*

trait LazoMSyntax {
  implicit final def lazoMSyntax[M, S](x: MessageData[M, S]): LazoMOps[M, S] = new LazoMOps(x)
}

final class LazoMOps[M, S](private val x: MessageData[M, S]) extends AnyVal {
  def fjs(implicit state: LazoState[M, S]): Set[M] =
    state.fjsOpt(x.mgjs).getOrElse(x.mgjs) // this orElse is for genesis case

  def selfJOpt(implicit state: LazoState[M, S]): Option[M] = state.selfJOpt(x.mgjs, x.sender)

  def seen(implicit state: LazoState[M, S]): Set[M] = state.view(x.mgjs)

  def baseBonds(implicit state: LazoState[M, S]): BondsMap[S] =
    state.bondsMap(x.mgjs).getOrElse(x.state.bonds)

  def lfIdx(implicit state: LazoState[M, S]): Option[Int] = state.lfIdxOpt(x.mgjs)

  def computeExtended(state: LazoState[M, S]): MessageData.Extended[M, S] = {
    implicit val s = state
    MessageData.Extended(x, Extra(fjs, selfJOpt, /*seen, */ baseBonds, lfIdx))
  }
}
