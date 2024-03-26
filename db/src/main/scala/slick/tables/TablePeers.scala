package slick.tables

import slick.jdbc.PostgresProfile.api.*
import slick.lifted.ProvenShape
import slick.tables.TablePeers.Peer

class TablePeers(tag: Tag) extends Table[Peer](tag, "peer") {
  def id: Rep[Long]             = column[Long]("id", O.AutoInc, O.PrimaryKey)
  def host: Rep[String]         = column[String]("host", O.Unique)
  def port: Rep[Int]            = column[Int]("port")
  def isSelf: Rep[Boolean]      = column[Boolean]("isSelf")
  def isValidator: Rep[Boolean] = column[Boolean]("isValidator")

  def idx = index("idx_peer_url", host, unique = true)

  override def * : ProvenShape[Peer] = (id, host, port, isSelf, isValidator).mapTo[Peer]
}

object TablePeers {
  final case class Peer(
    id: Long,
    host: String,
    port: Int,
    isSelf: Boolean,
    isValidator: Boolean,
  )
}
