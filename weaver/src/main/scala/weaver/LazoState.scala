package weaver

import cats.syntax.all.*
import weaver.LazoState.*
import weaver.Offence.InvalidFringe
import weaver.data.*
import weaver.rules.*
import weaver.rules.Dag.*
import weaver.syntax.all.*

import scala.collection.immutable.SortedMap

/** State supporting Lazo protocol. */
final case class LazoState[M, S](
  dagData: Map[M, DagData[M, S]],  // dag data
  exeData: Map[Int, ExeData[S]],   // execution data for all fringes
  fringes: Map[Int, Set[M]],       // all final fringes in the scope of Lazo
  fringesR: Map[Set[M], Int],      // all final fringes in the scope of Lazo
  seenMap: Map[M, Set[M]],         // all messages in the state seen by a message
  selfChildMap: Map[M, Option[M]], // self child (if exists) for a message,
  offences: Set[M],                // offences detected
  latestF: Int,
  latestMessages: Set[M],
  woSelfJ: Set[M],                 // messages without self parent - which means they are from newly bonded
  trustAssumption: FinalData[S],   // bonds and other data from the state that is trusted by the node.
  // It us either local bonds file and others or data read from the LFS which node is bootstrapping from
  traverser: Map[S, SortedMap[Int, M]],
) { self =>
  override def equals(obj: Any): Boolean = obj match {
    case x: LazoState[?, ?] => x.latestMessages == this.latestMessages
    case _                  => false
  }

  def add(
    id: M,
    m: MessageData.Extended[
      M,
      S,
    ], // TODO computation of seen should be done in this method, or `seen` truncation os required
    offenceOpt: Option[Offence],
  ): (LazoState[M, S], Set[M]) = {
    val fringeId       = offenceOpt match {
      case Some(InvalidBasic())      => FRINGE_IDX_INVALID
      case Some(InvalidFringe(_, _)) => FRINGE_IDX_INVALID
      case _                         =>
        val nextF        = latestF + 1
        val nextFringeId = if (nextF == F_ID_MAX) F_ID_MIN else nextF
        fringesR.getOrElse(m.fringes.fFringe, nextFringeId)
    }
    val newFringeIdOpt = (fringeId != FRINGE_IDX_INVALID && !fringes.contains(fringeId))
      .guard[Option]
      .as(fringeId)
    val newLatestF     = newFringeIdOpt.getOrElse(latestF)

    // TODO REMOVE DEBUG
    //  Prev fringe seeing new fringe is impossible so signalling a bug in finalization
    if (newFringeIdOpt.isDefined) {
      val see = fringes(latestF).flatMap(seenMap) intersect m.lazoM.finality.fFringe
      assert(
        see.isEmpty,
        s"\n$see \ncur ${fringes(latestF)} \n" +
          s"new ${m.lazoM.finality.fFringe} \n" +
          s"mgjs: \n ${m.lazoM.mgjs.map(x => x -> (dagData(x).fringeIdx -> fringes(dagData(x).fringeIdx))).toMap.mkString("\n ")}\n\n",
      )
    }

    val newOffences                   = offenceOpt.fold(offences)(_ => offences + id)
    // Attempt to prune the state. Laziness tolerance is read from the latest fringe in the
    // state before message adding.
    val (dataToPrune, fringesToPrune) = newFringeIdOpt
      .flatMap(_ => m.lfIdx)
      .map(exeData(_).lazinessTolerance)
      .fold((Set.empty[M], Set.empty[Int]))(prune(this, latestF, _))
    val newSeqNum                     = this.selfJOpt(m.mgj, m.sender).map(dagData(_).seqNum + 1).getOrElse(0)
    val newDagData                    = dagData + (id -> DagData(
      m.mgj,
      m.fjs,
      m.offences,
      fringeId,
      m.sender,
      newSeqNum,
    )) -- dataToPrune
    val newFringes                    = newFringeIdOpt.fold(fringes)(fId => fringes + (fId -> m.fringes.fFringe)) -- fringesToPrune
    val newFringesR                   = newFringeIdOpt.fold(fringesR)(fId => fringesR + (m.fringes.fFringe -> fId)) -- fringesToPrune
      .map(fringes)
    val newSelfChildMap               = m.selfJOpt
      .map(sjId => selfChildMap + (id -> none[M]) + (sjId -> id.some))
      .getOrElse(selfChildMap + (id -> none[M])) -- dataToPrune
    // TODO would be good not tp prune each value in a seenMap. But this is not strictly correct because
    //  can lead to a cycle situation when old messages see ids that are assigned to new ones.
    val mSeen                         = this.view(m.mgj)
    val newSeenMap                    =
      (seenMap + (id -> mSeen)).view.mapValues(_.filterNot(dataToPrune)).toMap -- dataToPrune
    val newExeData        = exeData + (fringeId -> ExeData(m.state.lazinessTolerance, m.state.bonds)) -- fringesToPrune
    val newLatestMessages = m.selfJOpt.foldLeft(latestMessages + id)(_ - _)
    val newWoSelfJ        = m.selfJOpt.fold(woSelfJ + id)(_ => woSelfJ) -- dataToPrune

    val newTraverser =
      traverser + (m.sender -> traverser.get(m.sender).map(_ + (newSeqNum -> id)).getOrElse(SortedMap(newSeqNum -> id)))

    val newLazo = copy(
      dagData = newDagData,
      exeData = newExeData,
      fringes = newFringes,
      fringesR = newFringesR,
      seenMap = newSeenMap,
      selfChildMap = newSelfChildMap,
      latestF = newLatestF,
      offences = newOffences,
      latestMessages = newLatestMessages,
      woSelfJ = newWoSelfJ,
      trustAssumption = trustAssumption,
      traverser = newTraverser,
    )

    (newLazo, dataToPrune)
  }

  lazy val latestMGJs: Set[M] = computeMGJS(latestMessages, (x: M, y: M) => seenMap.get(x).exists(_.contains(y)))

  def contains(m: M): Boolean = dagData.contains(m)

  def lookup(sender: S, seqNum: Int): Option[M] = traverser.get(sender).flatMap(_.get(seqNum))
}

object LazoState {
  // Fringe index for messages that are declared as invalid due to offences that prevent to compute valid fringe
  val FRINGE_IDX_INVALID = Int.MinValue
  val F_ID_MIN           = Int.MinValue + 1
  val F_ID_MAX           = Int.MaxValue

  /**
   * DAG data about the message.
   *
   * @param mgjs      minimal generative justifications
   * @param jss       justifications can be derived from mgjs and seen map, but it can be costly, so better to store it
   * @param fringeIdx index if the final fringe
   * @param sender    sender
   */
  final case class DagData[M, S](
    mgjs: Set[M],
    jss: Set[M],
    offences: Set[M],
    fringeIdx: Int,
    sender: S,
    seqNum: Int,
  )

  /** Data required for the protocol that should be provided by the execution engine. */
  final case class ExeData[S](lazinessTolerance: Int, bondsMap: Bonds[S])

  def empty[M, S](initExeData: FinalData[S]): LazoState[M, S] = new LazoState(
    dagData = Map.empty[M, DagData[M, S]],
    exeData = Map.empty[Int, ExeData[S]],
    fringes = Map(F_ID_MIN - 1 -> Set()),
    fringesR = Map.empty[Set[M], Int],
    seenMap = Map.empty[M, Set[M]],
    selfChildMap = Map.empty[M, Option[M]],
    latestF = F_ID_MIN - 1,
    offences = Set(),
    latestMessages = Set(),
    woSelfJ = Set(),
    trustAssumption = initExeData,
    traverser = Map.empty[S, SortedMap[Int, M]],
  )

//  /** Prune the state upon finding the new fringe. */
//  def prune[M, S](
//    state: Lazo[M, S],
//    latestFringeIdx: Int,
//    lazinessTolerance: Int,
//  ): (Set[M], Set[Int]) =
//    if (latestFringeIdx > F_ID_MIN + lazinessTolerance) {
//      // Pruned state won't be able to process any message with fringe below prune fringe.
//      val pruneFringeIdx = latestFringeIdx - lazinessTolerance
//      val dataToPrune    = state.fringes(pruneFringeIdx).flatMap(state.seenMap)
//      val fringesToPrune = state.fringes.collect { case (i, _) if i < pruneFringeIdx => i }
//      (dataToPrune, fringesToPrune.toSet)
//    } else Set.empty[M] -> Set.empty[Int]

  /** Prune the state upon finding the new fringe. */
  def prune[M, S](
    state: LazoState[M, S],
    latestFringeIdx: Int,
    lazinessTolerance: Int,
  ): (Set[M], Set[Int]) = {
    val latestFringe   = state.fringes(latestFringeIdx)
    val pruneFringeIdx =
      latestFringe.flatMap(state.selfChildMap).map(state.dagData(_).fringeIdx).minOption.getOrElse(F_ID_MIN)

    if (pruneFringeIdx > F_ID_MIN + lazinessTolerance + 1) {
      // Pruned state won't be able to process any message with fringe below prune fringe.
      val pruneFringeIdx = latestFringeIdx - (lazinessTolerance + 1)
      val dataToPrune    = state.fringes(pruneFringeIdx).flatMap(state.seenMap)
      val fringesToPrune = state.fringes.collect { case (i, _) if i < pruneFringeIdx => i }
      (dataToPrune, fringesToPrune.toSet)
    } else Set.empty[M] -> Set.empty[Int]
  }

  /** Whether message can be added to the state. */
  def canAdd[M, S](minGenJs: Set[M], sender: S, state: LazoState[M, S]): Boolean = {
    // whether all justifications are in the state
    val jsProcessed          = minGenJs.forall(state.dagData.contains)
    // genesis case
    lazy val genesisCase     = minGenJs.isEmpty && state.fringes.isEmpty
    // if state is pruned there might be not enough data to process the message anymore
    // exeData should not be pruned, so it can be used to check final data for all mgjs
    // TODO this is not pretty
    val exeNotPruned         = minGenJs.map(state.dagData).forall(x => state.fringes.contains(x.fringeIdx))
    // if an offender is detected in the view - there should not be any future message added;
    // this is necessary to meet the frugality basic rule.
    lazy val notFromOffender = {
      val selfJs             = state.selfJOpt(minGenJs, sender)
      val notFromEquivocator = selfJs.size <= 1
      lazy val selfJsIsValid = selfJs.forall(!state.offences.contains(_))
      notFromEquivocator && selfJsIsValid
    }
    (jsProcessed && exeNotPruned && notFromOffender) || genesisCase
  }

  /** Validate message for basic rules against the state. */
  def checkBasicRules[M, S](
    m: MessageData[M, S],
    state: LazoState[M, S],
  ): Option[InvalidBasic] = {
    val selfParentOpt  = m.selfJOpt(state)
    val latestFIdxOpt  = state.lfIdxOpt(m.mgjs)
    val bondsMap       = latestFIdxOpt.map(state.exeData(_).bondsMap).getOrElse(m.state.bonds)
    val justifications = computeFJS(
      m.mgjs,
      bondsMap.activeSet,
      state.dagData(_: M).jss,
      (x: M, y: M) => state.seenMap.get(x).exists(_.contains(y)),
      state.dagData(_: M).sender,
    )
    val seen           = (target: M) => m.mgjs.exists(state.seenMap(_).contains(target))
    val senderF        = state.dagData(_: M).sender
    Basic
      .validate(
        justifications,
        m.offences,
        selfParentOpt.map(state.dagData(_).mgjs).getOrElse(Set()),
        selfParentOpt.map(state.dagData(_).offences).getOrElse(Set()),
        justifications.map(j => j -> state.dagData(j).offences).toMap,
        senderF,
        seen,
      )
      .toOption
  }

  def isSynchronous[M, S](s: LazoState[M, S], self: S): Boolean = {
    val x = for {
      bonds   <- s.bondsMap(s.latestMGJs)
      latestM <- s.latestMessages.find(x => s.dagData(x).sender == self)
      newMS    = s.dagData(latestM).jss.flatMap(s.selfChildMap)
      x        = newMS ++ s.woSelfJ.diff(s.seenMap(latestM))
    } yield bonds.allAcross(x.map(s.dagData(_).sender))
    x.getOrElse(true)
  }
}
