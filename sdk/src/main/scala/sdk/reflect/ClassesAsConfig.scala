package sdk.reflect

import scala.reflect.runtime.universe.*

/** Render classes into configuration file for Typesafe Config */
object ClassesAsConfig {
  def apply(root: String, classes: Any*): String = classes
    .map { clz =>
      ClassAsTuple(clz)
        .map { case (name, value, anno) =>
          val formattedValue = value match {
            case s: String => s""""$s""""
            case l: Seq[_] => s"[\n  ${l.map(_.toString).mkString(",\n  ")}\n]"
            case _         => value.toString
          }
          s"""|# $anno
              |$root.${configName(clz)}.$name: $formattedValue
              |""".stripMargin
        }
        .mkString("")
    }
    .mkString(s"\n")

  def fields(x: Any): List[String] = ClassAsTuple(x).map { case (name, _, _) => name }.toList

  def configName(clz: Any): String = {
    val rm             = runtimeMirror(clz.getClass.getClassLoader)
    val instanceMirror = rm.reflect(clz)
    configName(instanceMirror.symbol.asClass)
  }

  def configName(classSymbol: ClassSymbol): String =
    classSymbol.annotations.head.tree.children.tail.head match {
      case Literal(Constant(value: String)) => value
      case _                                => "Could not extract annotation string"
    }
}
