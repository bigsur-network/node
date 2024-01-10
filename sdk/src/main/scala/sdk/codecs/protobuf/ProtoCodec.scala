package sdk.codecs.protobuf

import cats.syntax.all.*
import cats.{Applicative, Eval}
import com.google.protobuf.CodedOutputStream
import sdk.codecs.{Codec, PrimitiveReader, PrimitiveWriter}
import sdk.primitive.ByteArray

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream}
import scala.util.{Try, Using}

object ProtoCodec {

  def apply[A](
    writeA: A => (PrimitiveWriter[Eval] => Eval[Unit]),
    readA: PrimitiveReader[Eval] => Eval[A],
  ): Codec[A, ByteArray] =
    new Codec[A, ByteArray] {
      override def encode(x: A): Try[ByteArray] = Try(ByteArray(ProtoPrimitiveWriter.encodeWith(writeA(x)).value))
      override def decode(x: ByteArray): Try[A] = Try(ProtoPrimitiveReader.decodeWith[A](x.bytes, readA).value)
    }

  // TODO: make these functions more usable and elegant with cats.effect Resource and error handling

  // TODO: Properly handle errors
  @SuppressWarnings(Array("org.wartremover.warts.TryPartial"))
  def decode[F[_], T](bv: Array[Byte], read: InputStream => F[T]): F[T] =
    Using(new ByteArrayInputStream(bv))(read).get

  // TODO: Properly handle errors
  @SuppressWarnings(Array("org.wartremover.warts.TryPartial"))
  def encode[F[_]: Applicative](
    payloadSize: Int,
    write: CodedOutputStream => F[Unit],
  ): F[Array[Byte]] =
    Using(new ByteArrayOutputStream(payloadSize)) { baos =>
      val cos = CodedOutputStream.newInstance(baos)
      write(cos).map { _ =>
        cos.flush()
        baos.flush()
        baos.toByteArray
      }
    }.get
}
