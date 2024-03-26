package node.comm

import sdk.comm.Peer
import sdk.reflect.Description

@Description("comm")
final case class Config(
  @Description("Initial list of node peers")
  peers: List[Peer] = List.empty[Peer],
)

object Config {

  // Default config has an example value as a basis when filling config manually
  val Default: Config = Config(List.empty[Peer])
}
