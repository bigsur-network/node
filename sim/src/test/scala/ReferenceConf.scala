import node.ConfigManager
import org.scalatest.flatspec.AnyFlatSpec
import sdk.reflect.ClassesAsConfig

class ReferenceConf extends AnyFlatSpec {
  "this" should "output reference configuration for simulation" in {
    val s = ClassesAsConfig(
      ConfigManager.Namespace,
      sim.Config.Default,
      node.Config.Default,
      diagnostics.metrics.Config.Default,
      db.Config.Default,
      node.comm.Config.Default,
    )
    println(s)
  }
}
