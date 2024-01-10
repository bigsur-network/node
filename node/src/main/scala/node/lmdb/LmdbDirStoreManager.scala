package node.lmdb

import cats.effect.{Async, Deferred, Ref, Resource, Sync}
import cats.syntax.all.*
import node.lmdb.LmdbDirStoreManager.{Db, LmdbEnvConfig}
import sdk.store.{KeyValueStore, KeyValueStoreManager}

import java.nio.file.Path

object LmdbDirStoreManager {
  def apply[F[_]: Async](
    dirPath: Path,
    dbInstanceMapping: Map[Db, LmdbEnvConfig],
  ): Resource[F, KeyValueStoreManager[F]] =
    Resource.make(Sync[F].delay(new LmdbDirStoreManager(dirPath, dbInstanceMapping)))(_.shutdown)

  /**
    * Specification for LMDB database: unique identifier and database name
    *
    * @param id unique identifier
    * @param nameOverride name to use as database name instead of [[id]]
    */
  final case class Db(id: String, nameOverride: Option[String] = none)

  // Mega, giga and tera bytes
  private val mb = 1024L * 1024L
  private val gb = 1024L * mb
  val tb: Long   = 1024L * gb

  final case class LmdbEnvConfig(
    name: String,
    // Max LMDB environment (file) size
    maxEnvSize: Long = 1 * gb,
  )
}

// The idea for this class is to manage multiple of key-value lmdb databases.
// For LMDB this allows control which databases are part of the same environment (file).
final private case class LmdbDirStoreManager[F[_]: Async](
  dirPath: Path,
  dbMapping: Map[Db, LmdbEnvConfig],
) extends KeyValueStoreManager[F] {

  private case class StoreState(
    envs: Map[String, Deferred[F, KeyValueStoreManager[F]]],
  )

  private val managersState = Ref.unsafe(StoreState(Map.empty))

  private val dbInstanceMapping = dbMapping.map { case (db, cfg) => (db.id, (db, cfg)) }

  // Creates database uniquely defined by the name
  override def store(dbName: String): F[KeyValueStore[F]] =
    for {
      // Find DB ref / first time create deferred object
      newDefer                  <- Deferred[F, KeyValueStoreManager[F]]
      action                    <- managersState.modify { st =>
                                     val (db, cfg @ LmdbEnvConfig(manName, _)) = dbInstanceMapping(dbName)
                                     val (isNew, defer)                        = st.envs.get(manName).fold((true, newDefer))((false, _))
                                     val newSt                                 = st.copy(envs = st.envs.updated(manName, defer))
                                     (newSt, (isNew, defer, db, cfg))
                                   }
      (isNew, defer, db, manCfg) = action
      _                         <- createLmdbManger(manCfg, defer).whenA(isNew)
      manager                   <- defer.get
      dataBaseName               = db.nameOverride.getOrElse(db.id)
      database                  <- manager.store(dataBaseName)
    } yield database

  private def createLmdbManger(config: LmdbEnvConfig, defer: Deferred[F, KeyValueStoreManager[F]]): F[Unit] =
    for {
      manager <- LmdbStoreManager(dirPath.resolve(config.name), config.maxEnvSize)
      _       <- defer.complete(manager)
    } yield ()

  override def shutdown: F[Unit] = {
    import cats.instances.vector.*
    for {
      st <- managersState.get
      // Shutdown all store managers
      _  <- st.envs.values.toVector.traverse_(_.get.flatMap(_.shutdown))
    } yield ()
  }
}
