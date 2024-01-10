package node.api.web

import sdk.reflect.Description

@Description("webApi")
final case class Config(
  @Description("Host to listen on")
  host: String,
  @Description("Port to listen on")
  port: Int,
)
