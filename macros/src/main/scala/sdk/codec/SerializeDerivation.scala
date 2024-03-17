package sdk.codec

import cats.Applicative
import cats.syntax.all.*
import magnolia1.*
import sdk.codecs.*

/** 
 * Automatic derivation for Serialize type class.
 *
 * Serialization for sum types (traits) is not derived but provided by SerializeSumTypes mixin.
 * Check comment for commented out method `split` method for more details.
 * */
abstract class SerializeDerivation[F[_]: Applicative] extends SerializeSumTypes[F] with ExportedSerialize {
  type Typeclass[A] = Serialize[F, A]

  /**
   * For product types - traverse each field and serialize one after another.
   */
  def join[A](ctx: CaseClass[Serialize[F, *], A]): Serialize[F, A] =
    new Serialize[F, A] {
      override def write(x: A): PrimitiveWriter[F] => F[Unit] = (w: PrimitiveWriter[F]) =>
        ctx.parameters.traverse_ { p =>
          p.typeclass.write(p.dereference(x))(w)
        }

      override def read: PrimitiveReader[F] => F[A] = (r: PrimitiveReader[F]) =>
        ctx.parameters
          .traverse { p =>
            p.typeclass
              .read(r)
              .map(_.asInstanceOf[magnolia1.Param[sdk.codecs.Serialize[F, *], A]#PType])
          }
          .map(ctx.rawConstruct)
    }

  /**
   * Magnolia requires defining the `split` method to derive type classes for sealed traits.
   *
   * But for the case of Serialize type class, it does not make sense to derive serializers for traits
   * since instantiation of an object is required which is not possible for traits.
   *
   * So the commented out code below is just a placeholder to meet Magnolia documentation to better understand
   * this trait in future.
   *
   * This code is not required for compilation because macro expansion that uses it (implicit def gen[A] below)
   * should never be invoked. Type classes for all sum types should be defined above in [[SerializeSumTypes]].
   * */
//  def split[A](ctx: SealedTrait[Serialize[F, *], A]): Serialize[F, A] =
//    new Serialize[F, A] {
//      override def write(x: A): PrimitiveWriter[F] => F[Unit] = (_: PrimitiveWriter[F]) => ???
//      override def read: PrimitiveReader[F] => F[A]           = (_: PrimitiveReader[F]) => ???
//    }

  // Wrap the output of Magnolia in an Exported to force it to a lower priority.
  // Otherwise "ambiguous implicit values" error will rise since instance is already specified in the section above.
  implicit def gen[A]: Exported[Serialize[F, A]] = macro ExportedMagnolia.exportedMagnolia[Serialize[F, *], A]
}
