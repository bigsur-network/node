package dproc

import sdk.consensus.{ConflictResolution, ConsensusConfig}

object NodeRuntime {
  trait ExecutionRuntime[F[_], M, S] {
    // whether message replay was successful
    def checkReplay(m: M): F[Boolean]

    // data read from the final state associated with the final fringe
    def readConsensusData(fringe: Set[M]): F[Unit] // F[ConsensusConfig[S]]
  }

  trait ConsensusRuntime[F[_], M, S] {
    def computeFringe(minGenJs: Set[M]): Set[M]
    def add(m: M): F[Unit]
  }

  trait Resolver[F[_], M, S] {
    def resolve(top: Set[M]): Unit // ConflictResolution[Set[M], Set[M]]
  }

}
