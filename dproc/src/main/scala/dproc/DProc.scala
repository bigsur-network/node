package dproc

import cats.effect.kernel.Async
import cats.effect.std.{Mutex, Queue}
import cats.effect.{Ref, Sync}
import cats.syntax.all.*
import dproc.WeaverNode.*
import dproc.data.Block
import fs2.Stream
import sdk.DagCausalQueue
import sdk.diag.Metrics
import sdk.merging.Relation
import sdk.node.{Processor, Proposer}
import sdk.primitive.ByteArray
import weaver.WeaverState
import weaver.data.{ConflictResolution, FinalData}

/**
 * Process of an distributed computer.
 *
 * @param dProcStream main stream that launches the process
 * @param output stream of messages added to the state. Process should notify peers about this message.
 * @param gcStream stream of messages garbage collected
 * @param finStream stream of finalized transactions
 * @param acceptMsg callback to make process accept block that is fully received and stored
 * @param triggerPropose callback to trigger propose
 * @tparam M type of a message id
 * @tparam T type of a transaction id
 * @tparam F effect type
 */
final case class DProc[F[_], M, T](
  dProcStream: Stream[F, Unit],
  output: Stream[F, M],
  gcStream: Stream[F, Set[M]],
  finStream: Stream[F, ConflictResolution[T]],
  acceptMsg: M => F[Unit],
  triggerPropose: F[Unit],
)

object DProc {
  trait ExeEngine[F[_], M, S, T] {
    def execute(
      base: Set[M],
      finalFringe: Set[M],
      toFinalize: Set[T],
      toMerge: Set[T],
      txs: Set[T],
    ): F[((ByteArray, Seq[T]), (ByteArray, Seq[T]))]

    // data read from the final state associated with the final fringe
    def consensusData(fringe: Set[M]): F[FinalData[S]]
  }

  final case class WithState[F[_], M, S, T](
    dProc: DProc[F, M, T],
    weaverStRef: Ref[F, WeaverState[M, S, T]],
    procStRef: Ref[F, Processor.ST[M]],
    propStRef: Ref[F, Proposer.ST],
    bufferStRef: Ref[F, DagCausalQueue[M]],
  )

  final private case class BufferWithLock[F[_]: Sync, M, S, T](
    bufferStRef: Ref[F, DagCausalQueue[M]],
    weaverStRef: Ref[F, WeaverState[M, S, T]],
    mut: Mutex[F],
  ) {
    def add(m: M, dependencies: Set[M]): F[Set[M]] = mut.lock.surround(for {
      missing <- weaverStRef.get.map(w => dependencies.filterNot(w.lazo.contains))
      r       <- bufferStRef.modify(_.enqueue(m, missing).dequeue)
    } yield r)

    def remove(m: M): F[Set[M]] = mut.lock.surround(bufferStRef.modify(_.satisfy(m)._1.dequeue))
  }

  def apply[F[_]: Async: Metrics, M, S, T: Ordering](
    // states
    weaverStRef: Ref[F, WeaverState[M, S, T]], // weaver
    proposerStRef: Ref[F, Proposer.ST],        // proposer
    processorStRef: Ref[F, Processor.ST[M]],   // processor
    bufferStRef: Ref[F, DagCausalQueue[M]],    // buffer
    readTxs: F[Set[T]],                        // tx pool
    // id of the process if node is supposed to propose
    idOpt: Option[S],
    // execution engine
    exeEngine: ExeEngine[F, M, S, T],
    relation: Relation[F, T],
    // id generator for a block created
    idGen: Block[M, S, T] => F[M],
    // save block to persistent storage
    saveBlock: Block.WithId[M, S, T] => F[Unit],
    loadBlock: M => F[Block[M, S, T]],
  ): F[DProc[F, M, T]] = {

    val mkProposerOpt = idOpt.traverse { id =>
      val proposeF = proposeOnLatest(id, weaverStRef, readTxs, exeEngine, idGen).flatTap(saveBlock).map(_.id)
      Proposer[F, M](proposerStRef, proposeF)
    }

    def selfBlock(m: M) = idOpt.traverse(self => loadBlock(m).map(_.sender == self)).map(_.getOrElse(false))

    for {
      processor <- processor(weaverStRef, exeEngine, relation, processorStRef, loadBlock)
      proposer  <- mkProposerOpt.map(_.getOrElse(Stream.empty -> ().pure))
      buffer    <- Mutex[F].map(mut => BufferWithLock(bufferStRef, weaverStRef, mut))

      // queues for outbound data
      oQ  <- Queue.unbounded[F, M]                     // messages to be sent to peers
      gcQ <- Queue.unbounded[F, Set[M]]                // garbage collected messages
      fQ  <- Queue.unbounded[F, ConflictResolution[T]] // finalized transactions
    } yield {

      val (processedStream, triggerProcess) = processor
      val (proposedStream, triggerProp)     = proposer
      val triggerPropose                    = isSynchronousF(idOpt.get, weaverStRef).flatMap(triggerProp.whenA)

      def acceptIncoming(m: M): F[Unit] = for {
        b    <- loadBlock(m)
        next <- buffer.add(m, b.minGenJs ++ b.offences)
        _    <- next.toList.traverse(triggerProcess)
      } yield ()

      def addEffect(m: M, r: AddEffect[M, T]): F[Unit] = {
        val checkPanic = {
          val isSelfBlock = idOpt.traverse(id => loadBlock(m).map(_.sender).map(_ == id)).map(_.getOrElse(false))
          // Validation of self proposed message cannot fail. It is fatal otherwise.
          r.offenceOpt.traverse(o => Processor.haltIfSelfOffence(o.toString, isSelfBlock))
        }

        val effect = {
          val notifyProposer             = selfBlock(m).ifM(proposerStRef.modify(_.done).void, ().pure[F])
          val notifyBufferAndProcessNext = buffer.remove(m).flatMap(_.toList.traverse_(triggerProcess))
          val publishOutput              = oQ.offer(m) *> gcQ.offer(r.garbage) *> r.finalityOpt.traverse_(fQ.offer)
          notifyProposer *> notifyBufferAndProcessNext *> publishOutput *> triggerPropose
        }

        checkPanic *> effect
      }

      val s = processedStream.evalTap { case (m, r) => addEffect(m, r) }.void concurrently
        proposedStream.evalTap(triggerProcess)

      new DProc[F, M, T](
        s,
        Stream.fromQueueUnterminated(oQ),
        Stream.fromQueueUnterminated(gcQ),
        Stream.fromQueueUnterminated(fQ),
        acceptIncoming,
        triggerPropose,
      )
    }
  }
}
