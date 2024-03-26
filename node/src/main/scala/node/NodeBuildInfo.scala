package node

object NodeBuildInfo {
  def apply(): String = s"Node ${BuildInfo.version} (${BuildInfo.gitHeadCommit.getOrElse("commit # unknown")})"
}
