package node.rpc

import cats.Eval
import com.google.protobuf.CodedOutputStream
import io.grpc.MethodDescriptor
import org.apache.commons.io.input.QueueInputStream
import sdk.api.*
import sdk.codecs.Serialize
import sdk.codecs.protobuf.{ProtoPrimitiveReader, ProtoPrimitiveWriter}

import java.io.InputStream

/**
 * Constructor for Grpc method descriptor.
 * Used both for server and client.
 * */
object GrpcMethod {
  // We have to hardcode here effect type since this is the only way to connect
  // the code with parametrised effect type with gRPC implementation.
  // Current serializer implementation is based on Eval monad.
  type F[A] = Eval[A]

  // Stream using protobuf format
  private def streamProtobuf[A](obj: A)(implicit sA: Serialize[F, A]): InputStream = {
    // piped input stream
    val pipeIn      = new QueueInputStream()
    val pipeOut     = pipeIn.newQueueOutputStream()
    val protoStream = CodedOutputStream.newInstance(pipeOut)
    // writer that writes to output stream that pipes to input stream
    // write object using protobuf primitive writer
    sA.write(obj)(ProtoPrimitiveWriter.apply(protoStream)).value
    // flush and close
    protoStream.flush()
    pipeOut.flush()
    pipeOut.close()
    pipeIn
  }

  // Parse using protobuf format
  private def parseProtobuf[A](is: InputStream)(implicit sA: Serialize[F, A]): A = {
    val reader = ProtoPrimitiveReader.apply(is)
    sA.read(reader).value
  }

  def apply[A, B](endpointName: String)(implicit sA: Serialize[F, A], sB: Serialize[F, B]): MethodDescriptor[A, B] =
    MethodDescriptor
      .newBuilder()
      .setType(MethodDescriptor.MethodType.UNARY)
      .setFullMethodName(s"$RootPathString/$endpointName")
      .setRequestMarshaller(
        new MethodDescriptor.Marshaller[A] {
          override def stream(obj: A): InputStream       = streamProtobuf(obj)(sA)
          override def parse(byteStream: InputStream): A = parseProtobuf(byteStream)(sA)
        },
      )
      .setResponseMarshaller(new MethodDescriptor.Marshaller[B] {
        override def stream(obj: B): InputStream       = streamProtobuf(obj)(sB)
        override def parse(byteStream: InputStream): B = parseProtobuf(byteStream)(sB)
      })
      .build()
}
