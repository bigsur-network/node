package dproc

import cats.data.EitherT
import cats.effect.Ref
import cats.effect.kernel.{Async, Sync}
import cats.syntax.all.*
import cats.{Applicative, Monad}
import dproc.DProc.ExeEngine
import dproc.WeaverNode.{validateExeData, ReplayResult}
import dproc.data.Block
import fs2.Stream
import sdk.merging.{DagMerge, Relation, Resolve}
import sdk.node.Processor
import weaver.*
import weaver.GardState.GardM
import weaver.LazoState.isSynchronous
import weaver.Offence.*
import weaver.data.*
import weaver.rules.{Dag, Finality, InvalidBasic}
import weaver.syntax.all.*

final case class WeaverNode[F[_]: Sync, M, S, T](state: WeaverState[M, S, T]) {
  def dag: DagMerge[F, M] = new DagMerge[F, M] {
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

  def finalizedSet(minGenJs: Set[M], fFringe: Set[M]): F[Iterator[T]] = for {
    pf <- dag.latestFringe(minGenJs)
    r  <- dag.between(fFringe, pf).map(_.filterNot(state.lazo.offences)).map(_.flatMap(state.meld.txsMap))
  } yield r

  def resolver: Resolve[F, T] = new Resolve[F, T] {
    override def resolve(x: IterableOnce[T]): F[(Set[T], Set[T])] =
      Resolve.naive[T](x, state.meld.conflictsMap.contains).bimap(_.iterator.to(Set), _.iterator.to(Set)).pure
  }

  def computeFringe(minGenJs: Set[M]): FringeData[M] =
    Finality.tryAdvance(minGenJs, state.lazo).getOrElse(state.lazo.latestFringe(minGenJs))

  def computeFsResolve(fFringe: Set[M], minGenJs: Set[M]): F[ConflictResolution[T]] =
    for {
      toResolve <- finalizedSet(minGenJs, fFringe)
      r         <- resolver.resolve(toResolve)
    } yield ConflictResolution(r._1, r._2)

  def computeGard(txs: List[T], fFringe: Set[M], expT: Int): List[T] =
    txs.filterNot(state.gard.isDoubleSpend(_, fFringe, expT))

  def computeCsResolve(minGenJs: Set[M], fFringe: Set[M]): F[ConflictResolution[T]] = for {
    toResolve <- dag.between(minGenJs, fFringe).map(_.filterNot(state.lazo.offences).flatMap(state.meld.txsMap))
    r         <- resolver.resolve(toResolve)
  } yield ConflictResolution(r._1, r._2)

  def validateBasic(m: Block[M, S, T]): EitherT[F, InvalidBasic, Unit] = {
    val x = Sync[F].delay(LazoState.checkBasicRules(Block.toLazoM(m), state.lazo))
    EitherT(x.map(_.toLeft(())))
  }

  def validateFringe(m: Block[M, S, T]): EitherT[F, InvalidFringe[M], Set[M]] =
    EitherT(computeFringe(m.minGenJs).pure.map { case FringeData(fFringe) =>
      (fFringe != m.finalFringe)
        .guard[Option]
        .as(InvalidFringe(fFringe, m.finalFringe))
        .toLeft(m.finalFringe)
    })

  def validateFsResolve(m: Block[M, S, T]): EitherT[F, InvalidFringeResolve[T], ConflictResolution[T]] = {
    val is = m.finalized.getOrElse(ConflictResolution.empty[T])
    EitherT(computeFsResolve(m.finalFringe, m.minGenJs).map { shouldBe =>
      (shouldBe != is).guard[Option].as(InvalidFringeResolve(shouldBe, is)).toLeft(is)
    })
  }

  def validateGard(m: Block[M, S, T], expT: Int): EitherT[F, InvalidDoubleSpend[T], Unit] =
    EitherT(WeaverNode(state).computeGard(m.txs, m.finalFringe, expT).pure.map { txToPut =>
      (txToPut != m.txs).guard[Option].as(InvalidDoubleSpend(m.txs.toSet -- txToPut)).toLeft(())
    })

  def validateCsResolve(m: Block[M, S, T]): EitherT[F, InvalidResolution[T], Set[T]] =
    EitherT(WeaverNode(state).computeCsResolve(m.minGenJs, m.finalFringe).map { merge =>
      (merge.accepted != m.merge).guard[Option].as(InvalidResolution(merge.accepted)).toLeft(merge.accepted)
    })

  def createMessage(
    txs: List[T],
    sender: S,
    state: WeaverState[M, S, T],
    exeEngine: ExeEngine[F, M, S, T],
  ): F[Block[M, S, T]] = {

    val mgjs     = state.lazo.latestMGJs
    val offences = state.lazo.offences
    for {
      lazoF   <- Sync[F].delay(WeaverNode(state).computeFringe(mgjs))
      newF     = (lazoF.fFringe neqv state.lazo.latestFringe(mgjs).fFringe).guard[Option]
      fin     <- newF.traverse(_ => WeaverNode(state).computeFsResolve(lazoF.fFringe, mgjs))
      lazoE   <- exeEngine.consensusData(lazoF.fFringe)
      txToPut  = WeaverNode(state).computeGard(txs, lazoF.fFringe, lazoE.expirationThreshold)
      toMerge <- WeaverNode(state).computeCsResolve(mgjs, lazoF.fFringe)
      r       <- exeEngine.execute(
                   state.lazo.latestFringe(mgjs).fFringe,
                   lazoF.fFringe,
                   fin.map(_.accepted).getOrElse(Set()),
                   toMerge.accepted,
                   txToPut.toSet,
                 )

      ((finalStateHash, finRj), (postStateHash, provRj)) = r
    } yield Block(
      sender = sender,
      minGenJs = mgjs,
      txs = txToPut,
      offences = offences,
      finalFringe = lazoF.fFringe,
      // TODO add rejections due to negative balance overflow
      finalized = fin,          // .map(x => x.copy(rejected = x.rejected ++ finRj, accepted = x.accepted -- finRj)),
      merge = toMerge.accepted, // -- provRj,
      bonds = lazoE.bonds,
      lazTol = lazoE.lazinessTolerance,
      expThresh = lazoE.expirationThreshold,
      finalStateHash = finalStateHash,
      postStateHash = postStateHash,
    )
  }

  def validateMessage(
    m: Block[M, S, T],
    exeEngine: ExeEngine[F, M, S, T],
  ): EitherT[F, Offence, Unit] =
    for {
      _  <- WeaverNode(state).validateBasic(m)
      fr <- WeaverNode(state).validateFringe(m)
      _  <- WeaverNode(state).validateFsResolve(m)
      lE <- EitherT.liftF(exeEngine.consensusData(fr))
      _  <- validateExeData(lE, Block.toLazoE(m))
      _  <- validateGard(m, lE.expirationThreshold)
      _  <- validateCsResolve(m)

      toResolve <- EitherT.liftF(finalizedSet(m.minGenJs, m.finalFringe).map(_.toSet))
      base      <- EitherT.liftF(dag.latestFringe(m.minGenJs))

      r <- EitherT.liftF(exeEngine.execute(base, m.finalFringe, toResolve, m.merge, m.txs.toSet))

      ((finalState, _), (postState, _)) = r

      _ <- EitherT.fromOption(
             (finalState == m.finalStateHash && postState == m.postStateHash).guard[Option],
             Offence.iexec,
           )
    } yield ()

  def createBlockWithId(
    sender: S,
    txs: Set[T],
    exeEngine: ExeEngine[F, M, S, T],
    idGen: Block[M, S, T] => F[M],
  ): F[Block.WithId[M, S, T]] = for {
    // create message
    m <- createMessage(txs.toList, sender, state, exeEngine)
    // assign ID (hashing / signing done here)
    b <- idGen(m).map(id => Block.WithId(id, m))
  } yield b

  def replay(
    m: Block.WithId[M, S, T],
    exeEngine: ExeEngine[F, M, S, T],
    relations: Relation[F, T],
  ): F[ReplayResult[M, S, T]] = Sync[F].defer {
    lazy val conflictSet =
      Dag.between[M](m.m.minGenJs, m.m.finalFringe, state.lazo.seenMap).flatMap(state.meld.txsMap).toList
    lazy val unseen      = state.lazo.dagData.keySet -- state.lazo.view(m.m.minGenJs)
    lazy val mkMeld      = MeldState
      .computeRelationMaps[F, T](
        m.m.txs,
        unseen.flatMap(state.meld.txsMap).toList,
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

    val lazoME  = Block.toLazoM(m.m).computeExtended(state.lazo)
    val offOptT = validateMessage(m.m, exeEngine).swap.toOption

    // invalid messages do not participate in merge and are not accounted for double spend guard
    val offCase   = offOptT.map(off => ReplayResult(lazoME, none[MergingData[T]], none[GardM[M, T]], off.some))
    // if msg is valid - Meld state and Gard state should be updated
    val validCase = mkMeld.map(_.some).map(ReplayResult(lazoME, _, Block.toGardM(m.m).some, none[Offence]))

    offCase.getOrElseF(validCase)
  }
}

object WeaverNode {

  private def validateExeData[F[_]: Applicative](
    x: FinalData[?],
    ref: FinalData[?],
  ): EitherT[F, InvalidFringeState, Unit] =
    EitherT.fromOption((x == ref).guard[Option], InvalidFringeState())

  final case class ReplayResult[M, S, T](
    lazoME: MessageData.Extended[M, S],
    meldMOpt: Option[MergingData[T]],
    gardMOpt: Option[GardM[M, T]],
    offenceOpt: Option[Offence],
  )

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
      w  <- weaverStRef.get
      br <- WeaverNode(w).replay(b, exeEngine, relation)
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
    WeaverNode(w).createBlockWithId(sender, txs, exeEngine, idGen)
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
    loadBlock: M => F[Block[M, S, T]],
  ): F[(Stream[F, (M, AddEffect[M, T])], M => F[Unit])] = {
    def processF(m: M): F[Option[AddEffect[M, T]]] = for {
      b   <- loadBlock(m)
      bWId = Block.WithId(m, b)
      go  <- shouldAdd(bWId, weaverStRef)
      r   <- go.guard[Option].traverse(_ => replayAndAdd(bWId, weaverStRef, exeEngine, relation))
    } yield r

    Processor[F, M, Option[AddEffect[M, T]]](processorStRef, processF).map { case (out, in) =>
      out.collect { case (i, Some(o)) => i -> o } -> in
    }
  }
}
