package node.balances

import cats.Parallel
import cats.effect.Async
import cats.syntax.all.*
import dproc.DProc.ExeEngine
import dproc.data.Block
import node.codec.Hashing.*
import sdk.data.{BalancesDeploy, BalancesDeployBody, BalancesState}
import sdk.diag.Metrics
import sdk.history.History.EmptyRootHash
import sdk.primitive.ByteArray
import sdk.syntax.all.*
import weaver.data.FinalData

object Genesis {
  def mkGenesisBlock[F[_]: Async: Parallel: Metrics](
    sender: ByteArray,
    genesisExec: FinalData[ByteArray],
    genesisState: BalancesState,
    exeEngine: ExeEngine[F, ByteArray, ByteArray, BalancesDeploy],
  ): F[Block.WithId[ByteArray, ByteArray, BalancesDeploy]] = {
    val genesisDeploy = BalancesDeploy(BalancesDeployBody(genesisState, 0))
    exeEngine
      .execute(Set(), Set(), Set(), Set(), Set(genesisDeploy))
      .map { case (_, (postState, _)) =>
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
          finalStateHash = EmptyRootHash.bytes.bytes,
          postStateHash = postState.bytes,
        )

        Block.WithId(block.digest, block)
      }
  }
}
