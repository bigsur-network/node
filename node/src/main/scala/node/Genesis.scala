package node

import cats.Parallel
import cats.effect.Async
import cats.syntax.all.*
import dproc.data.Block
import node.Hashing.*
import sdk.serialize.auto.*
import sdk.data.{BalancesDeploy, BalancesDeployBody, BalancesState}
import sdk.diag.Metrics
import sdk.history.History
import sdk.history.History.EmptyRootHash
import sdk.primitive.ByteArray
import sdk.syntax.all.*
import weaver.data.FinalData

object Genesis {
  def mkGenesisBlock[F[_]: Async: Parallel: Metrics](
    sender: ByteArray,
    genesisExec: FinalData[ByteArray],
    genesisState: BalancesState,
    balanceShard: BalancesStateBuilderWithReader[F],
  ): F[Block.WithId[ByteArray, ByteArray, BalancesDeploy]] = {
    val genesisDeploy = BalancesDeploy(BalancesDeployBody(genesisState, 0))
    balanceShard
      .buildState(History.EmptyRootHash, BalancesState.Default, genesisDeploy.body.state)
      .map { case (_, postState) =>
        val block = Block[ByteArray, ByteArray, BalancesDeploy](
          sender,
          Set(),
          Set(),
          txs = List(genesisDeploy),
          Set(),
          None,
          Set(),
          genesisExec.bonds,
          genesisExec.lazinessTolerance,
          genesisExec.expirationThreshold,
          finalStateHash = EmptyRootHash.bytes,
          postStateHash = postState.bytes,
        )

        Block.WithId(block.digest, block)
      }
  }
}
