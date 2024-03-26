package sdk.comm

final case class Peer(
  host: String,
  port: Int,
  isSelf: Boolean,
  isValidator: Boolean,
) {
  override def equals(obj: Any): Boolean = obj match {
    case Peer(host, port, _, _) => this.host == host && this.port == port
    case _                      => false
  }

  override def hashCode(): Int = host.hashCode
}
