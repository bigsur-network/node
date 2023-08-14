package dproc

import cats.data.EitherT
import cats.effect.Ref
import cats.effect.kernel.{Async, Sync}
import cats.syntax.all.*
import cats.{Applicative, Monad}
import dproc.DProc.ExeEngine
import dproc.data.Block
import fs2.Stream
import sdk.consensus.Relation
import sdk.merging.{DagMerge, Resolve}
import sdk.node.Processor
import weaver.*
import weaver.GardState.GardM
import weaver.LazoState.isSynchronous
import weaver.Offence.*
import weaver.data.*
import weaver.rules.{Dag, Finality, InvalidBasic}
import weaver.syntax.all.*

final case class WeaverNode[M, S, T](state: WeaverState[M, S, T]) {
  def computeFringe(minGenJs: Set[M]): LazoF[M] =
    Finality.tryAdvance(minGenJs, state.lazo).getOrElse(state.lazo.latestFringe(minGenJs))

  def computeFsResolve[F[_]: Sync](fFringe: Set[M], minGenJs: Set[M])(
    dag: DagMerge[F, M],
    resolver: Resolve[F, T],
  ): F[ConflictResolution[T]] =
    for {
      pf        <- dag.latestFringe(minGenJs)
      toResolve <- dag.between(fFringe, pf).map(_.filterNot(state.lazo.offences).flatMap(state.meld.txsMap))
      r         <- resolver.resolve(toResolve)
    } yield ConflictResolution(r._1, r._2)

  def computeGard[F[_]: Sync](txs: List[T], fFringe: Set[M], expT: Int): List[T] =
    txs.filterNot(state.gard.isDoubleSpend(_, fFringe, expT))

  private def computeCsResolve[F[_]: Sync](minGenJs: Set[M], fFringe: Set[M])(
    dag: DagMerge[F, M],
    resolver: Resolve[F, T],
  ): F[ConflictResolution[T]] = for {
    toResolve <- dag.between(minGenJs, fFringe).map(_.filterNot(state.lazo.offences).flatMap(state.meld.txsMap))
    r         <- resolver.resolve(toResolve)
  } yield ConflictResolution(r._1, r._2)

  private def validateBasic[F[_]: Sync](m: Block[M, S, T]): EitherT[F, InvalidBasic, Unit] = {
    val x = Sync[F].delay(LazoState.checkBasicRules(Block.toLazoM(m), state.lazo))
    EitherT(x.map(_.toLeft(())))
  }

  private def validateFringe[F[_]: Sync](m: Block[M, S, T]): EitherT[F, InvalidFringe[M], Set[M]] =
    EitherT(computeFringe(m.minGenJs).pure.map { case LazoF(fFringe) =>
      (fFringe != m.finalFringe)
        .guard[Option]
        .as(InvalidFringe(fFringe, m.finalFringe))
        .toLeft(m.finalFringe)
    })

  private def validateFsResolve[F[_]: Sync](
    m: Block[M, S, T],
  )(dag: DagMerge[F, M], resolver: Resolve[F, T]): EitherT[F, InvalidFringeResolve[T], ConflictResolution[T]] =
    EitherT(computeFsResolve(m.finalFringe, m.minGenJs)(dag, resolver).map { finalization =>
      (finalization != m.finalized.getOrElse(ConflictResolution.empty[T]))
        .guard[Option]
        .as(InvalidFringeResolve(finalization, m.finalized.getOrElse(ConflictResolution.empty[T])))
        .toLeft(finalization)
    })

}
object WeaverNode {

  private def validateGard[F[_]: Sync, M, S, T](
    m: Block[M, S, T],
    s: WeaverState[M, S, T],
    expT: Int,
  ): EitherT[F, InvalidDoubleSpend[T], Unit] =
    EitherT(WeaverNode(s).computeGard(m.txs, m.finalFringe, expT).pure.map { txToPut =>
      (txToPut != m.txs).guard[Option].as(InvalidDoubleSpend(m.txs.toSet -- txToPut)).toLeft(())
    })

  private def validateCsResolve[F[_]: Sync, M, S, T: Ordering](
    m: Block[M, S, T],
    s: WeaverState[M, S, T],
  )(dag: DagMerge[F, M], resolver: Resolve[F, T]): EitherT[F, InvalidResolution[T], Set[T]] =
    EitherT(WeaverNode(s).computeCsResolve(m.minGenJs, m.finalFringe)(dag, resolver).map { merge =>
      (merge.accepted != m.merge).guard[Option].as(InvalidResolution(merge.accepted)).toLeft(merge.accepted)
    })

  private def validateExeData[F[_]: Applicative](
    x: FinalData[?],
    ref: FinalData[?],
  ): EitherT[F, InvalidFringeState, Unit] =
    EitherT.fromOption((x == ref).guard[Option], InvalidFringeState())

  private def createMessage[F[_]: Sync, M, S, T: Ordering](
    txs: List[T],
    sender: S,
    state: WeaverState[M, S, T],
    exeEngine: ExeEngine[F, M, S, T],
  ): F[Block[M, S, T]] = {
    val dag = new DagMerge[F, M] {
      override def between(ceil: Set[M], floor: Set[M]): F[Iterator[M]] = {
        val seqWithSender: M => (S, Int)  = (x: M) => state.lazo.dagData(x).sender -> state.lazo.dagData(x).seqNum
        val lookup: (S, Int) => Option[M] = (s: S, sN: Int) => state.lazo.lookup(s, sN)
        Dag.between[M, S](ceil, floor, seqWithSender, lookup).pure
      }

      override def latestFringe(target: Set[M]): F[Set[M]] =
        target
          .map(state.lazo.dagData(_).fringeIdx)
          .toList
          .maxOption
          .map(state.lazo.fringes)
          .getOrElse(Set.empty[M])
          .pure
    }

    val resolver = new Resolve[F, T] {
      override def resolve(x: IterableOnce[T]): F[(Set[T], Set[T])] =
        Resolve.naive[T](x, state.meld.conflictsMap.contains).bimap(_.iterator.to(Set), _.iterator.to(Set)).pure
    }

    val mgjs     = state.lazo.latestMGJs
    val offences = state.lazo.offences
    for {
      lazoF   <- Sync[F].delay(WeaverNode(state).computeFringe(mgjs))
      newF     = (lazoF.fFringe != state.lazo.latestFringe(mgjs)).guard[Option]
      fin     <- newF.traverse(_ => WeaverNode(state).computeFsResolve(lazoF.fFringe, mgjs)(dag, resolver))
      lazoE   <- exeEngine.consensusData(lazoF.fFringe)
      txToPut  = WeaverNode(state).computeGard(txs, lazoF.fFringe, lazoE.expirationThreshold)
      toMerge <- WeaverNode(state).computeCsResolve(mgjs, lazoF.fFringe)(dag, resolver)
      _       <- exeEngine.execute(lazoF.fFringe, toMerge.accepted, txToPut) // TODO return hash here to put in a block
    } yield Block(
      sender = sender,
      minGenJs = mgjs,
      txs = txToPut,
      offences = offences,
      finalFringe = lazoF.fFringe,
      finalized = fin,
      merge = toMerge.accepted,
      bonds = lazoE.bonds,
      lazTol = lazoE.lazinessTolerance,
      expThresh = lazoE.expirationThreshold,
    )
  }

  private def validateMessage[F[_]: Sync, M, S, T: Ordering](
    m: Block[M, S, T],
    s: WeaverState[M, S, T],
    exeEngine: ExeEngine[F, M, S, T],
  )(dag: DagMerge[F, M], resolver: Resolve[F, T]): EitherT[F, Offence, Unit] =
    for {
      _  <- WeaverNode(s).validateBasic(m)
      fr <- WeaverNode(s).validateFringe(m)
      _  <- WeaverNode(s).validateFsResolve(m)(dag, resolver)
      lE <- EitherT.liftF(exeEngine.consensusData(fr))
      _  <- validateExeData(lE, Block.toLazoE(m))
      _  <- validateGard(m, s, lE.expirationThreshold)
      _  <- validateCsResolve(m, s)(dag, resolver)
      _  <- EitherT(exeEngine.execute(m.finalFringe, m.merge, m.txs).map(_.guard[Option].toRight(Offence.iexec)))
    } yield ()

  private def createBlockWithId[F[_]: Sync, M, S, T: Ordering](
    sender: S,
    s: WeaverState[M, S, T],
    txs: Set[T],
    exeEngine: ExeEngine[F, M, S, T],
    idGen: Block[M, S, T] => F[M],
  ): F[Block.WithId[M, S, T]] = for {
    // create message
    m <- createMessage[F, M, S, T](txs.toList, sender, s, exeEngine)
    // assign ID (hashing / signing done here)
    b <- idGen(m).map(id => Block.WithId(id, m))
  } yield b

  final private case class ReplayResult[M, S, T](
    lazoME: MessageData.Extended[M, S],
    meldMOpt: Option[MergingData[T]],
    gardMOpt: Option[GardM[M, T]],
    offenceOpt: Option[Offence],
  )

  private def replay[F[_]: Sync, M, S, T: Ordering](
    m: Block.WithId[M, S, T],
    s: WeaverState[M, S, T],
    exeEngine: ExeEngine[F, M, S, T],
    relations: Relation[F, T],
  )(dag: DagMerge[F, M], resolver: Resolve[F, T]): F[ReplayResult[M, S, T]] = Sync[F].defer {
    lazy val conflictSet = Dag.between(m.m.minGenJs, m.m.finalFringe, s.lazo.seenMap).flatMap(s.meld.txsMap).toList
    lazy val unseen      = s.lazo.dagData.keySet -- s.lazo.view(m.m.minGenJs)
    lazy val mkMeld      = MeldState
      .computeRelationMaps[F, T](
        m.m.txs,
        unseen.flatMap(s.meld.txsMap).toList,
        conflictSet,
        relations.conflicts,
        relations.depends,
      )
      .map { case (cm, dm) =>
        MergingData(
          m.m.txs,
          conflictSet.toSet,
          cm,
          dm,
          m.m.finalized.map(_.accepted).getOrElse(Set()),
          m.m.finalized.map(_.rejected).getOrElse(Set()),
        )
      }

    val lazoME  = Block.toLazoM(m.m).computeExtended(s.lazo)
    val offOptT = validateMessage(m.m, s, exeEngine)(dag, resolver).swap.toOption

    // invalid messages do not participate in merge and are not accounted for double spend guard
    val offCase   = offOptT.map(off => ReplayResult(lazoME, none[MergingData[T]], none[GardM[M, T]], off.some))
    // if msg is valid - Meld state and Gard state should be updated
    val validCase = mkMeld.map(_.some).map(ReplayResult(lazoME, _, Block.toGardM(m.m).some, none[Offence]))

    offCase.getOrElseF(validCase)
  }
  final case class AddEffect[M, T](
    garbage: Set[M],
    finalityOpt: Option[ConflictResolution[T]],
    offenceOpt: Option[Offence],
  )

  /**
   * Replay block and add it to the Weaver state.
   */
  private def replayAndAdd[F[_]: Sync, M, S, T: Ordering](
    b: Block.WithId[M, S, T],
    weaverStRef: Ref[F, WeaverState[M, S, T]],
    exeEngine: ExeEngine[F, M, S, T],
    relation: Relation[F, T],
  ): F[AddEffect[M, T]] =
    for {
      w <- weaverStRef.get

      dag = new DagMerge[F, M] {
              override def between(ceil: Set[M], floor: Set[M]): F[Iterator[M]] = {
                val seqWithSender: M => (S, Int)  = (x: M) => w.lazo.dagData(x).sender -> w.lazo.dagData(x).seqNum
                val lookup: (S, Int) => Option[M] = (s: S, sN: Int) => w.lazo.lookup(s, sN)

                Dag.between[M, S](ceil, floor, seqWithSender, lookup).pure
              }

              override def latestFringe(target: Set[M]): F[Set[M]] =
                target
                  .map(w.lazo.dagData(_).fringeIdx)
                  .toList
                  .maxOption
                  .map(w.lazo.fringes)
                  .getOrElse(Set.empty[M])
                  .pure
            }

      naiveResolver = new Resolve[F, T] {
                        override def resolve(x: IterableOnce[T]): F[(Set[T], Set[T])] = weaverStRef.get
                          .map(w => Resolve.naive[T](x, w.meld.conflictsMap.contains))
                          .map(_.bimap(_.iterator.to(Set), _.iterator.to(Set)))
                      }

      br <- replay(b, w, exeEngine, relation)(dag, naiveResolver)
      r  <- weaverStRef.modify(_.add(b.id, br.lazoME, br.meldMOpt, br.gardMOpt, br.offenceOpt))
      _  <- new Exception(s"Add failed after replay which should not be possible.").raiseError.unlessA(r._2)
    } yield AddEffect(r._1, b.m.finalized, br.offenceOpt)

  /**
   * Whether block should be added to the state enclosed in Ref.
   *
   * This should be called when full block is received before attempting to add.
   */
  private def shouldAdd[F[_]: Sync, M, S, T](
    b: Block.WithId[M, S, T],
    weaverStRef: Ref[F, WeaverState[M, S, T]],
  ): F[Boolean] =
    weaverStRef.get.map(w => WeaverState.shouldAdd(w.lazo, b.id, b.m.minGenJs ++ b.m.offences, b.m.sender))

  /**
   * Propose given the latest state of the node.
   */
  def proposeOnLatest[F[_]: Sync, M, S, T: Ordering](
    sender: S,
    weaverStRef: Ref[F, WeaverState[M, S, T]],
    readTxs: => F[Set[T]],
    exeEngine: ExeEngine[F, M, S, T],
    idGen: Block[M, S, T] => F[M],
  ): F[Block.WithId[M, S, T]] = (weaverStRef.get, readTxs).flatMapN { case (w, txs) =>
    createBlockWithId(sender, w, txs, exeEngine, idGen)
  }

  def isSynchronousF[F[_]: Monad, M, S, T](id: S, weaverStRef: Ref[F, WeaverState[M, S, T]]): F[Boolean] =
    weaverStRef.get.map(w => isSynchronous(w.lazo, id))

  /**
   * Stream of processed blocks with callback to trigger process
   */
  def processor[F[_]: Async, M, S, T: Ordering](
    weaverStRef: Ref[F, WeaverState[M, S, T]],
    exeEngine: ExeEngine[F, M, S, T],
    relation: Relation[F, T],
    processorStRef: Ref[F, Processor.ST[M]],
    loadBlock: M => F[Block.WithId[M, S, T]],
  ): F[(Stream[F, (M, AddEffect[M, T])], M => F[Unit])] = {
    def processF(m: M): F[Option[AddEffect[M, T]]] = for {
      b  <- loadBlock(m)
      go <- shouldAdd(b, weaverStRef)
      r  <- go.guard[Option].traverse(_ => replayAndAdd(b, weaverStRef, exeEngine, relation))
    } yield r

    Processor[F, M, Option[AddEffect[M, T]]](processorStRef, processF).map { case (out, in) =>
      out.collect { case (i, Some(o)) => i -> o } -> in
    }
  }
}

//def computeConflicts[F[_] : Applicative, T](t: T, unseen: List[T], conflictsF: (T, T) => F[Boolean]): F[(T, List[T])] =
//  unseen.traverseFilter(x => conflictsF(t, x).ifF(x.some, None)).map(t -> _)
//
//final case class BranchingMap[T](private val branchMap: Map[T, Set[T]]) extends DependencyTracker[Id, T] {
//  def add(x: T, dependencies: Set[T]): BranchingMap[T] = {
//    val newV = LazyList
//      .unfold((branchMap, dependencies)) { case (acc, d) =>
//        val nextD = d.flatMap(acc.getOrElse(_, Set()))
//        val nextAcc = d.foldLeft(acc)((acc, i) => acc.updated(i, acc.getOrElse(i, Set()) + x))
//        if (nextD.isEmpty) None else (nextAcc -> (nextAcc, nextD)).some
//      }
//      .last
//    BranchingMap(newV)
//  }
//
//  def dependsOn(x: T, on: T): Boolean = branchMap.get(on).exists(_.contains(x))
//}
//
//final case class ConflictsMap[T](private val conflictsMap: Map[T, Set[T]]) extends Conflicts[Id, T] {
//  def add(x: T, conflicts: Set[T]): ConflictsMap[T] = {
//    val newV = conflicts.foldLeft(conflictsMap.updated(x, conflicts)) { case (acc, i) =>
//      acc.updated(i, acc.getOrElse(i, Set()) + x)
//    }
//    ConflictsMap(newV)
//  }
//
//  def conflictsWith(x: T, y: T): Boolean = conflictsMap.get(y).exists(_.contains(x))
//
//  def conflictsWithAny(x: T): Boolean = conflictsMap.contains(x)
//}
//}
