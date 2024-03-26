package node

import cats.Parallel
import cats.effect.Async
import cats.syntax.all.*
import fs2.Stream
import node.rpc.syntax.all.grpcClientSyntax
import node.rpc.{GrpcChannelsManager, GrpcClient}
import sdk.crypto.ECDSA
import sdk.data.{BalancesState, HostWithPort}
import sdk.diag.Metrics
import sdk.log.Logger.*
import sdk.primitive.ByteArray
import secp256k1.Secp256k1
import weaver.data.FinalData

import java.net.InetSocketAddress
import scala.concurrent.duration.{DurationInt, FiniteDuration}

object Node {

  def createGenesis[F[_]: Async: Parallel](
    id: ByteArray,
    genesisPoS: FinalData[ByteArray],
    genesisBalances: BalancesState,
    setup: Setup[F],
  ): Stream[F, Unit] = {
    implicit val m: Metrics[F] = Metrics.unit
    Stream.eval(for {
      _ <- logInfoF(s"Creating genesis block.")
      g <- Genesis.mkGenesisBlock[F](id, genesisPoS, genesisBalances, setup.balancesShard)
      _ <- logDebugF(s"Genesis block created with hash ${g.id}.")
      _ <- DbApiImpl(setup.database).saveBlock(g)
      _ <- logDebugF(s"Genesis block saved.")
      _ <- setup.dProc.acceptMsg(g.id)
      _ <- logInfoF(s"Genesis complete.")
    } yield ())
  }

  private def waitTillResolved[F[_]: Async](
    isa: InetSocketAddress,
    time: FiniteDuration = 2.second,
  ): F[InetSocketAddress] =
    isa.tailRecM { isa =>
      if (isa.isUnresolved)
        logInfoF(s"Bootstrap host ${isa.getHostName} is unavailable. Retry in ${time.toSeconds} seconds.") *>
          Async[F].sleep(time) *>
          Async[F].delay(new InetSocketAddress(isa.getHostName, isa.getPort).asLeft[InetSocketAddress])
      else
        Async[F].delay(isa.asRight[InetSocketAddress])
    }

  private def waitTillLatestMessageIsAvailableAndGet[F[_]: Async: GrpcChannelsManager](
    bootstrap: HostWithPort,
    time: FiniteDuration = 2.second,
  ): F[Seq[ByteArray]] =
    ().tailRecM { _ =>
      // TODO make API methods return errors?
      GrpcClient[F]
        .getLatestBlocks(bootstrap)
        .flatTap(x => new Exception(s"Empty latest messages.").raiseError.whenA(x.isEmpty))
        .attempt
        .flatMap {
          case Right(lms) =>
            logInfoF(
              s"Query for latest messages from $bootstrap succeed: $lms",
            ) *> Async[F].delay(lms.asRight[Unit])
          case Left(e)    =>
            logInfoF(
              s"Query for latest messages from $bootstrap failed: ${e.getLocalizedMessage}. Retry in $time seconds.",
            ) *> Async[F].sleep(time).as(().asLeft[Seq[ByteArray]])
        }
    }

  private def bootstrap[F[_]: Async: GrpcChannelsManager](
    bootstrap: HostWithPort,
    blockResolver: BlockResolver[F],
  ): Stream[F, Unit] = Stream.eval {
    for {
      resolved <- waitTillResolved(InetSocketAddress.createUnresolved(bootstrap.host, bootstrap.port))
      tips     <- waitTillLatestMessageIsAvailableAndGet(bootstrap)
      _        <- tips.traverse(blockResolver.in(_, bootstrap))
    } yield ()
  }

  def apply[F[_]: Async: Parallel](
    id: ByteArray,
    isBootstrap: Boolean,
    setup: Setup[F],
    genesisPoS: FinalData[ByteArray],
    genesisBalances: BalancesState,
    bootstrapOpt: Option[HostWithPort] = None,
  ): fs2.Stream[F, Unit] = {
    val printDiag: Stream[F, Unit] = NodeSnapshot
      .stream(setup.stateManager, setup.dProc.finStream)
      .metered(3000.milli)
      .evalMap(x => logDebugF(x.show))

    val init =
      if (isBootstrap) createGenesis[F](id, genesisPoS, genesisBalances, setup)
      else {
        implicit val gcm: GrpcChannelsManager[F] = setup.grpcChannelsManager
        bootstrap(bootstrapOpt.get, setup.blockResolver)
      }

    val run: Stream[F, Unit] = setup.nodeStream

    (init ++ printDiag) concurrently run
  }

  val SupportedECDSA: Map[String, ECDSA] = Map("secp256k1" -> Secp256k1.apply)
  val BalancesShardName: String          = "balances"
}
