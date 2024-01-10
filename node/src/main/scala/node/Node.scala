package node

import cats.Parallel
import cats.effect.{Async, Ref, Resource}
import cats.syntax.all.*
import dproc.DProc
import dproc.DProc.ExeEngine
import dproc.data.Block
import node.codec.Hashing.*
import sdk.DagCausalQueue
import sdk.api.data.Balance
import sdk.crypto.ECDSA
import sdk.data.{BalancesDeploy, BalancesState}
import sdk.diag.Metrics
import sdk.history.{ByteArray32, History}
import sdk.merging.MergeLogicForPayments.mergeRejectNegativeOverflow
import sdk.merging.Relation
import sdk.node.{Processor, Proposer}
import sdk.primitive.ByteArray
import secp256k1.Secp256k1
import weaver.WeaverState
import weaver.data.FinalData

import java.nio.file.Path

final case class Node[F[_]](
  // state references
  weaverStRef: Ref[F, WeaverState[ByteArray, ByteArray, BalancesDeploy]],
  procStRef: Ref[F, Processor.ST[ByteArray]],
  propStRef: Ref[F, Proposer.ST],
  bufferStRef: Ref[F, DagCausalQueue[ByteArray]],
  // inputs and outputs
  dProc: DProc[F, ByteArray, BalancesDeploy],
  // balanceShard
  exeEngine: ExeEngine[F, ByteArray, ByteArray, BalancesDeploy],
)

object Node {

  final case class State(
    weaver: WeaverState[ByteArray, ByteArray, BalancesDeploy],
    proposer: Proposer.ST,
    processor: Processor.ST[ByteArray],
    buffer: DagCausalQueue[ByteArray],
    lfsHash: ByteArray32,
  )

  val SupportedECDSA: Map[String, ECDSA] = Map("secp256k1" -> Secp256k1.apply)

  /** Make instance of a process - peer or the network.
   * Init with last finalized state (lfs as the simplest). */
  def apply[F[_]: Async: Parallel: Metrics](
    id: ByteArray,
    lfs: WeaverState[ByteArray, ByteArray, BalancesDeploy],
    hash: Block[ByteArray, ByteArray, BalancesDeploy] => F[ByteArray],
    dataDir: Path,
    blockStore: (
      Block.WithId[ByteArray, ByteArray, BalancesDeploy] => F[Unit],
      ByteArray => F[Option[Block[ByteArray, ByteArray, BalancesDeploy]]],
    ),
    txToInclude: F[Set[BalancesDeploy]],
    nodeCfg: Config,
  ): Resource[
    F,
    (
      Node[F],
      (
        F[ByteArray32],
        (ByteArray32, ByteArray) => F[Option[Balance]],
      ),
    ),
  ] =
    Setup.lmdbBalancesShard(dataDir).evalMap { balancesEngine =>
      for {
        weaverStRef    <- Ref.of(lfs)                               // weaver
        proposerStRef  <- Ref.of(Proposer.default)                  // proposer
        processorStRef <- Ref.of(Processor.default[ByteArray]())    // processor
        bufferStRef    <- Ref.of(DagCausalQueue.default[ByteArray]) // buffer

        (blockStorePut, blockStoreRead) = blockStore

        fringeMappingRef <- Ref.of(Map.empty[Set[ByteArray], ByteArray32])
        txPoolRef        <- Ref.of(Map.empty[ByteArray, BalancesState])

        exeEngine = new ExeEngine[F, ByteArray, ByteArray, BalancesDeploy] {
                      def execute(
                        base: Set[ByteArray],
                        fringe: Set[ByteArray],
                        toFinalize: Set[BalancesDeploy],
                        toMerge: Set[BalancesDeploy],
                        txs: Set[BalancesDeploy],
                      ): F[((ByteArray, Seq[BalancesDeploy]), (ByteArray, Seq[BalancesDeploy]))] =
                        for {
                          baseState <- fringeMappingRef.get.map(_.getOrElse(base, History.EmptyRootHash))

                          r <- mergeRejectNegativeOverflow(
                                 balancesEngine.readBalance,
                                 baseState,
                                 toFinalize,
                                 toMerge ++ txs,
                               )

                          ((newFinState, finRj), (newMergeState, provRj)) = r

                          r <- balancesEngine.buildState(baseState, newFinState, newMergeState)

                          (finalHash, postHash) = r

                          _ <- fringeMappingRef.update(_ + (fringe -> finalHash)).unlessA(fringe.isEmpty)
                        } yield ((finalHash.bytes, finRj), (postHash.bytes, provRj))

                      // data read from the final state associated with the final fringe
                      def consensusData(fringe: Set[ByteArray]): F[FinalData[ByteArray]] =
                        lfs.lazo.trustAssumption.pure[F] // TODO
                    }

        dProc <- DProc.apply[F, ByteArray, ByteArray, BalancesDeploy](
                   weaverStRef,
                   proposerStRef,
                   processorStRef,
                   bufferStRef,
                   readTxs = txToInclude,
                   id.some,
                   exeEngine,
                   Relation.notRelated[F, BalancesDeploy],
                   hash,
                   blockStorePut,
                   blockStoreRead.andThen(_.map(_.get)),
                 )

        lfsHashF = (fringeMappingRef.get, weaverStRef.get).mapN { case (fM, w) =>
                     w.lazo.fringes
                       .minByOption { case (i, _) => i }
                       .map { case (_, fringe) => fringe }
                       .flatMap(fM.get)
                       .getOrElse(History.EmptyRootHash)
                   }

        readBalance = balancesEngine.readBalance(_: ByteArray32, _: ByteArray).map(_.map(new Balance(_)))
        saveTx      = (tx: BalancesDeploy) => txPoolRef.update(_.updated(tx.id, tx.body.state))
        txLookup    = (id: ByteArray) => txPoolRef.get.map(_.get(id))

        api = Setup.webApi(
                blockStoreRead,
                nodeCfg.webApi.host,
                nodeCfg.webApi.port,
                devMode = nodeCfg.devMode,
                saveTx,
                txLookup,
                readBalance,
                weaverStRef.get.map(_.lazo.latestMessages),
              )
      } yield new Node(
        weaverStRef,
        processorStRef,
        proposerStRef,
        bufferStRef,
        dProc.copy(dProcStream = dProc.dProcStream concurrently api),
        exeEngine,
      ) -> (lfsHashF, readBalance)
    }
}
