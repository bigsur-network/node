package node

import cats.effect.kernel.{Resource, Sync}
import cats.syntax.all.*
import fs2.io.file.Files
import io.circe.{parser, Decoder, Encoder}
import sdk.codecs.Base16
import sdk.comm.Peer
import sdk.primitive.ByteArray

import scala.io.{BufferedSource, Source}

object FileLoaders {

  def getContent[F[_]: Sync](path: String): Resource[F, BufferedSource] =
    Resource.make(Sync[F].delay(Source.fromFile(path)))(s => Sync[F].delay(s.close()))

  def loadPoSFile[F[_]: Sync: Files](path: String): F[Map[ByteArray, Long]] = loadWalletsFile[F](path)

  def loadWalletsFile[F[_]: Sync: Files](path: String): F[Map[ByteArray, Long]] =
    getContent(path).use { source =>
      parser
        .decode[Map[String, Long]](source.mkString)
        .liftTo[F]
        .flatMap { wallets =>
          wallets.toList
            .traverse { case (k, v) => Base16.decode(k).map(ByteArray(_)).liftTo[F].map((_, v)) }
            .map(_.toMap)
        }
    }

  def loadPeers[F[_]: Sync: Files](path: String): F[List[Peer]] =
    getContent(path).use { source =>
      implicit val peerCodec: Decoder[Peer] = Decoder.forProduct4("host", "port", "isSelf", "isValidator")(Peer.apply)
      parser.decode[List[Peer]](source.mkString).liftTo[F]
    }
}
