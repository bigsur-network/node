package rhonix

/** Package describing API. HTTP, RPC, any other possible protocol.
 * This package defines routes, authentication, servers abstracted from the actual data being served
 * */
package object api {
  private val namespace = "io.rhonix.api.v1"
  private val version   = "v2"
  val prefix            = s"$namespace.$version"
}
