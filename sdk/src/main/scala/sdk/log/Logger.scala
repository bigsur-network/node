package sdk.log

import cats.effect.kernel.Sync

import scala.reflect.macros.blackbox

/**
 * Static functions for logging.
 *
 * {{{
 *  com.typesafe.scalalogging.Logger is used as a logging backend.
 *  This object contains also macro expansion to provide log source for typesafe Logger automatically.
 *
 *  Usage: insert the following import statement at the beginning of your file
 *
 *  import sdk.log.Logger.*
 *  
 *  to get access log functions with log sources automatically derived.
 * }}}
 * */
object Logger {
  import com.typesafe.scalalogging.Logger as L

  def logDebug(msg: String)(implicit ev: LogSource): Unit = L(ev.clazz).debug(msg)

  def logDebugF[F[_]: Sync](msg: String)(implicit ev: LogSource): F[Unit] = Sync[F].delay(logDebug(msg)(ev))

  def logInfo(msg: String)(implicit ev: LogSource): Unit = L(ev.clazz).info(msg)

  def logInfoF[F[_]: Sync](msg: String)(implicit ev: LogSource): F[Unit] = Sync[F].delay(logInfo(msg)(ev))

  def logError(msg: String)(implicit ev: LogSource): Unit = L(ev.clazz).error(msg)

  def logErrorF[F[_]: Sync](msg: String)(implicit ev: LogSource): F[Unit] = Sync[F].delay(logError(msg)(ev))

  @SuppressWarnings(Array("org.wartremover.warts.Null"))
  implicit def matLogSource: LogSource = macro LogSourceMacros.mkLogSource

  private class LogSourceMacros(val c: blackbox.Context) {
    import c.universe.*

    def mkLogSource: c.Expr[LogSource] = {
      val tree = q"""sdk.log.LogSource(${c.reifyEnclosingRuntimeClass}.asInstanceOf[Class[_]])"""
      c.Expr[LogSource](tree)
    }
  }
}
