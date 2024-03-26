package node

import cats.effect.*
import cats.effect.kernel.Ref.Make
import cats.effect.std.{Env, Random}
import cats.syntax.all.*
import node.Hashing.*
import sdk.serialize.auto.*
import sdk.codecs.Base16
import sdk.data.{BalancesDeploy, BalancesDeployBody, BalancesState, HostWithPort}
import sdk.error.FatalError
import sdk.log.Logger.*
import sdk.primitive.ByteArray
import sdk.reflect.ClassesAsConfig
import sdk.syntax.all.*
import slick.SlickPgDatabase
import weaver.data.{Bonds, FinalData}

import java.net.InetAddress

object Main extends IOApp {
  private val DbUrlKey      = "gorki_db_url"
  private val DbUser        = "gorki_db_user"
  private val DbPasswd      = "gorki_db_passwd"
  private val ValidatorId   = "gorki_validator_id"
  private val BootstrapAddr = "bootstrap"

  private val PosFilePath     = "/var/lib/gorki/genesis/pos.json"
  private val WalletsFilePath = "/var/lib/gorki/genesis/wallets.json"
  private val PeersFilePath   = "/var/lib/gorki/comm/peers.json"

  def randomDeploys[F[_]: Make: Sync: Random](
    users: Set[ByteArray],
    n: Int,
  ): F[Set[BalancesDeploy]] = {
    val mkDeploy = for {
      txVal <- Random[F].nextLongBounded(100)
      from  <- Random[F].elementOf(users)
      to    <- Random[F].elementOf(users - from)
    } yield {
      val st = new BalancesState(Map(from -> -txVal, to -> txVal))
      val bd = BalancesDeployBody(st, 0)
      val id = bd.digest
      BalancesDeploy(id, bd)
    }
    mkDeploy.replicateA(n).map(_.toSet)
  }

  private def configWithEnv[F[_]: Env: Sync]: F[(db.Config, String)] = {
    val dbConfF = for {
      dbUrlOpt      <- Env[F].get(DbUrlKey)
      dbUserOpt     <- Env[F].get(DbUser)
      dbPasswordOpt <- Env[F].get(DbPasswd)
    } yield for {
      dbUrl      <- dbUrlOpt
      dbUser     <- dbUserOpt
      dbPassword <- dbPasswordOpt
    } yield db.Config.Default.copy(url = dbUrl, user = dbUser, password = dbPassword)

    (
      dbConfF.map(_.getOrElse(db.Config.Default)),
      Env[F]
        .get(ValidatorId)
        .flatMap(_.liftTo[F](new FatalError(s"Environment variable $ValidatorId is not set"))),
    ).bisequence
  }

  override def run(args: List[String]): IO[ExitCode] =
    args match {
      case List("--print-default-config") =>
        val referenceConf = ClassesAsConfig(
          "gorki",
          db.Config.Default,
          node.Config.Default,
        )
        IO.println(referenceConf).as(ExitCode.Success)
      case _                              =>
        val mainStreamF = for {
          _               <- logInfoF[IO](s"Node version: ${NodeBuildInfo()}")
          c               <- configWithEnv[IO]
          bootstrpOpt     <- Env[IO].get(BootstrapAddr)
          (dbCfg, nodeId)  = c
          genesisPoS      <- FileLoaders.loadJsonMap[IO](PosFilePath).map(x => FinalData(Bonds(x), 1, 10000))
          genesisBalances <- FileLoaders.loadJsonMap[IO](WalletsFilePath)
          peers           <- FileLoaders.loadPeers[IO](PeersFilePath)
          random          <- Random.scalaUtilRandom[IO]
        } yield {
          val nodePK                   = Base16.decode(nodeId).map(ByteArray(_)).getUnsafe
          implicit val rnd: Random[IO] = random
          val dummyTxs                 = randomDeploys[IO](genesisBalances.keySet, 1)

          val localHostname = InetAddress.getLocalHost.getCanonicalHostName

          // read peer list
          val peerList = peers.filterNot(_.host == localHostname)

          Setup
            .all[IO](SlickPgDatabase[IO](dbCfg), nodePK, genesisPoS, dummyTxs, 0, localHostname)
            .use { s =>
              val updatePeersFromFile =
                s.peerManager.all.flatMap { peersMap =>
                  val toRemove = peersMap.values.map(p => p.host -> p.port).toSet
                  s.peerManager.remove(toRemove)
                } *> s.peerManager.add(peerList.map(p => (p.host, p.port) -> p).toMap)

              val logLoadedPeers = s.peerManager.all.flatMap { ps =>
                logInfoF[IO](s"Loaded peers: [${ps.keys.map { case (host, port) => s"$host:$port" }.mkString(", ")}]")
              }

              @SuppressWarnings(Array("org.wartremover.warts.Throw"))
              val bootOpt = bootstrpOpt.map(_.split(":")).map {
                case Array(host, port) => HostWithPort(host, port.toInt)
                case _                 => throw new FatalError("Invalid bootstrap address")
              }

              updatePeersFromFile *> logLoadedPeers *>
                Node
                  .apply[IO](nodePK, bootstrpOpt.isEmpty, s, genesisPoS, BalancesState(genesisBalances), bootOpt)
                  .compile
                  .drain
                  .as(ExitCode.Success)
            }
        }

        mainStreamF.flatten
    }
}
