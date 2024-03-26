package node.api

import cats.data.ValidatedNel
import cats.effect.kernel.Sync
import sdk.api.data.{Block, Deploy, Status, TokenTransferRequest}
import sdk.api.{ApiErr, ExternalApi}
import sdk.primitive.ByteArray
import slick.api.SlickApi
import cats.syntax.all.*
import node.{BalancesStateBuilderWithReader, NodeBuildInfo}
import node.api.web.Validation
import sdk.codecs.Digest
import sdk.data.{BalancesDeploy, BalancesDeployBody, BalancesState}
import sdk.history.ByteArray32
import sdk.store.KeyValueTypedStore
import sdk.serialize.auto.*
import node.Hashing.*
import sdk.syntax.all.*

object ExternalApiSlickImpl {
  def apply[F[_]: Sync](
    database: SlickApi[F],
    b: BalancesStateBuilderWithReader[F],
    latestM: F[List[ByteArray]],
    deployPool: KeyValueTypedStore[F, ByteArray, BalancesDeploy],
  ): ExternalApi[F] =
    new ExternalApi[F] {
      override def getBlockByHash(hash: Array[Byte]): F[Option[Block]] =
        database.blockGet(ByteArray(hash)).map(_.map(sdk.api.data.Block.apply))

      override def getDeployByHash(hash: Array[Byte]): F[Option[Deploy]] =
        database.deployGet(ByteArray(hash)).map(_.map(Deploy.apply))

      override def getDeploysByHash(hash: Array[Byte]): F[Option[Seq[Array[Byte]]]] = ???

      override def getBalance(state: Array[Byte], wallet: Array[Byte]): F[Option[Long]] = for {
        ba      <- Sync[F].fromTry(ByteArray32.convert(state))
        balance <- b.readBalance(ba, ByteArray(wallet))
      } yield balance

      override def getLatestMessages: F[List[Array[Byte]]] = latestM.map(_.map(_.bytes))

      override def status: F[Status] = Status(NodeBuildInfo()).pure[F]

      override def transferToken(tx: TokenTransferRequest): F[ValidatedNel[ApiErr, Unit]] =
        Validation
          .validateTokenTransferRequest(tx) /*(implicitly[Digest[TokenTransferRequest.Body]])*/
          .traverse { _ =>
            deployPool
              .put(
                ByteArray(tx.digest),
                BalancesDeploy(
                  ByteArray(tx.signature),
                  BalancesDeployBody(
                    BalancesState(
                      Map(
                        ByteArray(tx.body.from) -> -tx.body.value,
                        ByteArray(tx.body.to)   -> tx.body.value,
                      ),
                    ),
                    0L,
                  ),
                ),
              )
          }
    }
}
