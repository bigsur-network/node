package sdk.codecs

import cats.Monad
import cats.syntax.all.*

/**
 * Specification for serialization of sum types.
 * */
trait SerializeSumTypes[F[_]] {

  implicit def serializeOpt[A](implicit
    serializeA: Serialize[F, A],
    monad: Monad[F],
  ): Serialize[F, Option[A]] =
    new Serialize[F, Option[A]] {
      override def write(x: Option[A]): PrimitiveWriter[F] => F[Unit] =
        (w: PrimitiveWriter[F]) =>
          x match {
            case Some(a) => w.write(true) *> serializeA.write(a)(w)
            case None    => w.write(false)
          }

      override def read: PrimitiveReader[F] => F[Option[A]] =
        (r: PrimitiveReader[F]) =>
          for {
            exists <- r.readBool
            value  <- if (exists) serializeA.read(r).map(Some(_)) else None.pure[F]
          } yield value
    }

  implicit def serializeList[A: Ordering](implicit
    sA: Serialize[F, A],
    monad: Monad[F],
  ): Serialize[F, List[A]] =
    new Serialize[F, List[A]] {
      override def write(x: List[A]): PrimitiveWriter[F] => F[Unit] =
        (w: PrimitiveWriter[F]) =>
          // just `traverse_` cannot be used
          // See more examples here: https://gist.github.com/nzpr/38f843139224d1de1f0e9d15c75e925c
          w.write(x.size) *> x.sorted.map(sA.write(_)(w)).fold(().pure[F])(_ *> _)

      override def read: PrimitiveReader[F] => F[List[A]] =
        (r: PrimitiveReader[F]) =>
          for {
            size  <- r.readInt
            value <- (0 until size).toList.traverse(_ => sA.read(r))
          } yield value
    }

  implicit def serializeSet[A: Ordering](implicit
    sA: Serialize[F, A],
    monad: Monad[F],
  ): Serialize[F, Set[A]] =
    new Serialize[F, Set[A]] {
      override def write(x: Set[A]): PrimitiveWriter[F] => F[Unit] =
        serializeList[A].write(x.toList)

      override def read: PrimitiveReader[F] => F[Set[A]] =
        serializeList[A].read(_).map(_.toSet)
    }

  implicit def serializeMap[A: Ordering, B](implicit
    sA: Serialize[F, (A, B)],
    monad: Monad[F],
  ): Serialize[F, Map[A, B]] = {
    // order map records by keys
    implicit val tupleOrd: Ordering[(A, B)] = Ordering.by[(A, B), A](_._1)

    new Serialize[F, Map[A, B]] {
      override def write(bonds: Map[A, B]): PrimitiveWriter[F] => F[Unit] =
        serializeList[(A, B)].write(bonds.toList)

      override def read: PrimitiveReader[F] => F[Map[A, B]] =
        serializeList[(A, B)].read(_).map(_.toMap)
    }
  }
}
