package weaver.syntax

import sdk.consensus.data.BondsMap
import weaver.data.*

trait LazoMESyntax {
  implicit final def lazoMESyntax[M, S](x: MessageData.Extended[M, S]): LazoMEOps[M, S] =
    new LazoMEOps(x)
}

final class LazoMEOps[M, S](private val x: MessageData.Extended[M, S]) extends AnyVal {
  def sender: S = x.lazoM.sender

  def mgj: Set[M] = x.lazoM.mgjs

  def offences: Set[M] = x.lazoM.offences

  def fringes: FringeData[M] = x.lazoM.finality

  def state: FinalData[S] = x.lazoM.state

  def fjs: Set[M] = x.ext.fjs

  def selfJOpt: Option[M] = x.ext.selfJOpt

//  def seen: Set[M] = x.ext.seen

  def baseBonds: BondsMap[S] = x.ext.baseBonds

  def lfIdx: Option[Int] = x.ext.lfIdx
}
