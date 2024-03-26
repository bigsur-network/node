package node

import cats.effect.{Async, Resource, Sync}
import cats.syntax.all.*
import com.typesafe.config.ConfigFactory
import diagnostics.*
import io.circe.parser.parse
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder, Json}
import pureconfig.{CamelCase, ConfigFieldMapping, ConfigSource}
import pureconfig.generic.ProductHint
import sdk.error.FatalError
import sdk.log.Logger.*
import sdk.reflect.ClassesAsConfig
import slick.api.SlickApi

import java.nio.file.Path
import scala.concurrent.duration.Duration

// Logic to store and load config from the database
object ConfigManager {
  val DefaultConfig: (Config, metrics.Config, comm.Config) =
    (node.Config.Default, metrics.Config.Default, comm.Config.Default)

  // Field to check that database is empty so should be populated with default config
  val EmptyCheckKey = "isEmpty"

  val Namespace = "gorki"

  // Codecs to represent Path and Duration as strings, Circe does not have this out of the box
  implicit val ePath: Encoder[Path]             = Encoder.encodeString.contramap(_.toString)
  implicit val dPath: io.circe.Decoder[Path]    = io.circe.Decoder.decodeString.map(x => Path.of(x))
  implicit val eDur: Encoder[Duration]          = Encoder.encodeLong.contramap(_.toNanos)
  implicit val dDur: io.circe.Decoder[Duration] = io.circe.Decoder.decodeLong.map(s => Duration.fromNanos(s))

  /**
   * Write config into database
   * @param x instance of a config class. Should be annotated, ptal [[node.Config]]
   * @tparam A type of the config class
   * @return
   */
  def writeConfig[F[_]: Async, A: Encoder.AsObject](x: A, db: SlickApi[F]): F[Unit] = {
    val configName = ClassesAsConfig.configName(x)
    x.asJsonObject.toList.traverse_ { case (k, v) => db.putConfig(s"$configName.$k", v.noSpaces) }
  }

  /**
   * Update annotated config of type A with records from from database.
   */
  def loadConfig[F[_]: Async, A: Decoder](x: A, db: SlickApi[F]): F[A] = {
    def noKeyErr(key: String) = new Exception(s"No config for $key")
    val configName            = ClassesAsConfig.configName(x)
    val keys                  = ClassesAsConfig.fields(x)
    keys
      .traverse { key =>
        val dbKey = s"$configName.$key" // Key in the database is written with config prefix
        for {
          value     <- db.getConfig(dbKey).flatMap(_.liftTo[F](noKeyErr(dbKey)))
          jsonValue <- parse(value).liftTo[F]
        } yield key -> jsonValue
      }
      .flatMap(Json.fromFields(_).as[A].liftTo[F])
  }

  // Write all configs into database and set empty check flag
  def writeAll[F[_]: Async](nCfg: node.Config, mCfg: metrics.Config, cCfg: comm.Config, db: SlickApi[F]): F[Unit] = {
    import io.circe.generic.auto.*
    writeConfig(nCfg, db) *> writeConfig(mCfg, db) *> writeConfig(cCfg, db) *> db.putConfig(EmptyCheckKey, "")
  }

  // Load all configs from database
  def loadAll[F[_]: Async](db: SlickApi[F]): F[(node.Config, metrics.Config, comm.Config)] = {
    import io.circe.generic.auto.*
    (
      loadConfig(node.Config.Default, db),
      loadConfig(metrics.Config.Default, db),
      loadConfig(comm.Config.Default, db),
    ).tupled
  }

  def loadReference[F[_]: Sync]: F[(Config, metrics.Config, comm.Config)] = {
    import pureconfig.generic.auto.*
    implicit def productHint[T]: ProductHint[T] = ProductHint[T](ConfigFieldMapping(CamelCase, CamelCase))

    val configEither = for {
      nConfig <- {
        val cfg = ConfigFactory
          .defaultReference()
          .getConfig(Namespace)
          .getConfig(ClassesAsConfig.configName(node.Config.Default))
        ConfigSource.fromConfig(cfg).load[node.Config].liftTo
      }
      mConfig <- {
        val cfg = ConfigFactory
          .defaultReference()
          .getConfig(Namespace)
          .getConfig(ClassesAsConfig.configName(diagnostics.metrics.Config.Default))
        ConfigSource.fromConfig(cfg).load[diagnostics.metrics.Config].liftTo
      }
      cConfig <- {
        val cfg = ConfigFactory
          .defaultReference()
          .getConfig(Namespace)
          .getConfig(ClassesAsConfig.configName(comm.Config.Default))
        ConfigSource.fromConfig(cfg).load[comm.Config].liftTo
      }
    } yield (nConfig, mConfig, cConfig)

    configEither.leftMap(f => new FatalError(f.toString())).liftTo[F]
  }

  /**
   * Build node configuration.
   *
   * If the database is empty, the default configuration is written to it.
   * If the database is not empty - configuration is loaded from the database.
   * */
  def buildConfig[F[_]: Async](
    db: SlickApi[F],
  ): Resource[F, (node.Config, metrics.Config, comm.Config)] = Resource.eval(Sync[F].defer {
    for {
      isEmpty <- db.getConfig(EmptyCheckKey).map(_.isEmpty)
      r       <- if (isEmpty) for {
                   cfg <- loadReference
                   _   <- writeAll(cfg._1, cfg._2, cfg._3, db)
                 } yield cfg
                 else loadAll(db)
    } yield r
  })
}
