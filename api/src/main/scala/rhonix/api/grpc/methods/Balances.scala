package rhonix.api.grpc.methods

import io.grpc.*
import rhonix.api.grpc.methods.protobuf.*
import rhonix.api.grpc.serviceName

import java.io.InputStream
object Balances {

  private val methodName = "Balances"
  case class Request(wallet: String)
  case class Response(balance: Long)

  private val requestMarshaller = new MethodDescriptor.Marshaller[Request] {
    override def stream(obj: Request): InputStream       = writeRequestProtobuf(obj)
    override def parse(byteStream: InputStream): Request = readRequestProtobuf(byteStream)
  }

  private val responseMarshaller = new MethodDescriptor.Marshaller[Response] {
    override def stream(obj: Response): InputStream       = writeResponseProtobuf(obj)
    override def parse(byteStream: InputStream): Response = readResponseProtobuf(byteStream)
  }

  def balancesMethodDescriptor: MethodDescriptor[Request, Response] = MethodDescriptor
    .newBuilder()
    .setType(MethodDescriptor.MethodType.UNARY)
    // Method name with the namespace prefix
    .setFullMethodName(s"$serviceName/$methodName")
    // Encoder/decoder for input and output types
    .setRequestMarshaller(requestMarshaller)
    .setResponseMarshaller(responseMarshaller)
    .build()

  def balancesServerCallHandler(getBalance: String => Long): ServerCallHandler[Request, Response] =
    new ServerCallHandler[Request, Response] {
      override def startCall(
        call: ServerCall[Request, Response],
        headers: Metadata,
      ): ServerCall.Listener[Request] = {
        // Number of messages to read next from the response (default is no read at all)
        call.request(1)

        // Server listener of clients requests
        new ServerCall.Listener[Request] {
          // Handle client request message and optionally respond
          override def onMessage(message: Request): Unit = {
            val balance = getBalance(message.wallet)
            // Sends headers
            call.sendHeaders(headers)
            // Sends the message
            call.sendMessage(Response(balance))
            // Close must be called, but only once to prevent exception
            call.close(Status.OK, headers)
          }

          override def onHalfClose(): Unit = ()

          override def onCancel(): Unit = ()

          override def onComplete(): Unit = ()
        }
      }
    }
}
