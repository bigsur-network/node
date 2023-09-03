package rhonix.api.grpc.methods

import com.google.protobuf.{CodedInputStream, CodedOutputStream}
import rhonix.api.grpc.methods.Balances.{Request, Response}

import java.io.{InputStream, PipedInputStream, PipedOutputStream}

/// Protobuf marshalling/unmarshalling for gRPC Methods
package object protobuf {

  /// Input stream given writer to output stream. Safe to writer failure.
  private def writeProtobufBracket(writer: CodedOutputStream => Unit): PipedInputStream = {
    val pipeInput = new PipedInputStream
    val pipeOut   = new PipedOutputStream(pipeInput)
    val outStream = CodedOutputStream.newInstance(pipeOut)
    try writer(outStream)
    finally {
      outStream.flush()
      pipeOut.flush()
      pipeOut.close()
    }
    pipeInput
  }

  def writeRequestProtobuf(obj: Request): InputStream =
    writeProtobufBracket(out => out.writeStringNoTag(obj.wallet))

  def readRequestProtobuf(in: InputStream): Request = {
    val protoStream = CodedInputStream.newInstance(in)
    val balance     = protoStream.readString()
    Request(balance)
  }

  def writeResponseProtobuf(obj: Response): InputStream =
    writeProtobufBracket(out => out.writeFixed64NoTag(obj.balance))

  def readResponseProtobuf(in: InputStream): Response = {
    val protoStream = CodedInputStream.newInstance(in)
    val balance     = protoStream.readFixed64()
    Response(balance)
  }
}
