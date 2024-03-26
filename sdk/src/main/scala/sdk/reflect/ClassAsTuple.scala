package sdk.reflect

import cats.effect.Sync

import scala.reflect.runtime.currentMirror
import scala.reflect.runtime.universe.*
import scala.util.Try

object ClassAsTuple {
  def apply(cc: Any): Iterable[(String, Any, String)] = {
    val rm             = runtimeMirror(cc.getClass.getClassLoader)
    val instanceMirror = rm.reflect(cc)
    val classSymbol    = instanceMirror.symbol.asClass

    val annotations = classSymbol.primaryConstructor.typeSignature.paramLists.head.map { s =>
      val annotationString = s.annotations.head.tree.children.tail.head match {
        case Literal(Constant(value: String)) => value
        case _                                => "Could not extract annotation string"
      }
      s.name.decodedName.toString -> annotationString
    }.toMap

    val fields = classSymbol.typeSignature.members.collect { case m: MethodSymbol if m.isCaseAccessor => m }

    fields.toList.sortBy(_.name.toString).map { fieldSymbol =>
      val fieldName        = fieldSymbol.name.toString
      val fieldValue       = instanceMirror.reflectField(fieldSymbol.asTerm).get
      val fieldDescription = annotations.getOrElse(fieldName, "No description provided")

      (fieldName, fieldValue, fieldDescription)
    }
  }

  @SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
  def fromMap[F[_]: Sync, T: TypeTag](root: String, map: Map[String, Any]): F[T] = {
    val classSymbol       = typeOf[T].typeSymbol.asClass
    val classMirror       = currentMirror.reflectClass(classSymbol)
    val constructorSymbol = typeOf[T].decl(termNames.CONSTRUCTOR).asMethod
    val constructorMirror = classMirror.reflectConstructor(constructorSymbol)

    val constructorParams = constructorSymbol.paramLists.flatten

    Sync[F].fromTry {
      Try {
        val constructorArgs = constructorParams.map { param =>
          val paramType = param.typeSignature
          val paramName = param.name.toString
          val cfgName   = ClassesAsConfig.configName(classSymbol)
          val key       = s"$root.$cfgName.$paramName"
          val value     = map(key)

          paramType match {
            case t if t =:= typeOf[String]    => value.asInstanceOf[String]
            case t if t =:= typeOf[Int]       => value.asInstanceOf[Double].toInt
            case t if t =:= typeOf[Double]    => value.asInstanceOf[Double]
            case t if t =:= typeOf[Boolean]   => value.asInstanceOf[Boolean]
            case t if t <:< typeOf[List[?]]   => value.asInstanceOf[List[Any]]
            case t if t <:< typeOf[Set[?]]    => value.asInstanceOf[Set[Any]]
            case t if t <:< typeOf[Map[?, ?]] => value.asInstanceOf[Map[Any, Any]]
            case t                            => t.asInstanceOf[paramType.type]
          }
        }
        constructorMirror(constructorArgs*).asInstanceOf[T]
      }
    }
  }
}
