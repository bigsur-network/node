package node

import cats.effect.{Resource, Sync}
import sdk.primitive.ByteArray
import sdk.syntax.all.*
import weaver.data.Bonds

import scala.io.Source
import scala.util.Try

object BondsMap {
  def fromFile[F[_]: Sync](filePath: String): F[Bonds[ByteArray]] =
    Resource
      .make(
        Sync[F].delay(Source.fromFile(filePath)),
      )(source => Sync[F].delay(source.close()))
      .use { source =>
        Sync[F].delay {
          val bondsMap = source
            .getLines()
            .map { line =>
              val parts = line.split(" ")
              Try {
                val pubKey = parts.head.decodeHex.map(ByteArray.apply).toOption.get
                val stake  = parts.last.toLong
                pubKey -> stake
              }.toOption
            }
            .collect { case Some((pubKey, stake)) => pubKey -> stake }
            .toMap
          Bonds(bondsMap)
        }
      }
}
