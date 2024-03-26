package node.state

import cats.Monad
import cats.effect.Ref
import cats.effect.kernel.Ref.Make
import cats.syntax.all.*
import sdk.DagCausalQueue
import sdk.data.BalancesDeploy
import sdk.history.ByteArray32
import sdk.node.{Processor, Proposer}
import sdk.primitive.ByteArray
import weaver.WeaverState

final case class StateManager[F[_]](
  // state references
  weaverStRef: Ref[F, WeaverState[ByteArray, ByteArray, BalancesDeploy]],
  procStRef: Ref[F, Processor.ST[ByteArray]],
  propStRef: Ref[F, Proposer.ST],
  bufferStRef: Ref[F, DagCausalQueue[ByteArray]],
  fringeMappingRef: Ref[F, Map[Set[ByteArray], ByteArray32]],
)

object StateManager {
  def apply[F[_]: Monad: Make](lfs: WeaverState[ByteArray, ByteArray, BalancesDeploy]): F[StateManager[F]] = for {
    // inMem state
    weaverStRef      <- Ref.of(lfs)                                        // weaver
    proposerStRef    <- Ref.of(Proposer.default)                           // proposer
    processorStRef   <- Ref.of(Processor.default[ByteArray]())             // processor
    bufferStRef      <- Ref.of(DagCausalQueue.default[ByteArray])          // buffer
    fringeMappingRef <- Ref.of[F, Map[Set[ByteArray], ByteArray32]](Map()) // mapping of fringes to their state hashes
  } yield new StateManager(weaverStRef, processorStRef, proposerStRef, bufferStRef, fringeMappingRef)
}
