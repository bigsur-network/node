package sdk

package object api {
  private val Namespace      = List("api")
  val Version                = "v1"
  val RootPath: Seq[String]  = Namespace :+ Version
  val Title: String          = "Gorki node API"
  val DocFileName: String    = "openapi.json"
  val RootPathString: String = RootPath.mkString("_")

  val BlockHashEndpoint    = "BlockHash"
  val BlockEndpoint        = "Block"
  val LatestBlocksEndpoint = "LatestBlock"
}
