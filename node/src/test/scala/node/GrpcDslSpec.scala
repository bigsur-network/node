package node

import com.google.protobuf.{CodedInputStream, CodedOutputStream}
import io.grpc.*
import io.grpc.netty.{NettyChannelBuilder, NettyServerBuilder}
import org.apache.commons.io.input.QueueInputStream
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.io.{InputStream, PipedInputStream, PipedOutputStream}
import java.time.LocalDateTime

class GrpcDslSpec extends AnyFlatSpec with Matchers {

  // Object sent over gRPC
  case class MyObj(text: String, num: Int)

  // Encoding functions using streams (using protobuf serializer)
  // NOTE: here low level validation can happen on the field level or even byte level,
  //       e.g. validating the message header like version or signature
  val myObjMarshal = new MethodDescriptor.Marshaller[MyObj] {
    override def stream(obj: MyObj): InputStream = {
      val pipeInput   = new QueueInputStream()
      val pipeOut     = pipeInput.newQueueOutputStream()
      // Writes serialized fields to output stream (input stream for gRPC)
      val protoStream = CodedOutputStream.newInstance(pipeOut)
      protoStream.writeString(1, obj.text)
      protoStream.writeInt32(2, obj.num)
      protoStream.flush()
      pipeOut.flush()
      pipeOut.close()
      pipeInput
    }

    override def parse(byteStream: InputStream): MyObj = {
      // Deserialize input byte stream to object fields
      val protoStream = CodedInputStream.newInstance(byteStream)
      protoStream.readTag()
      val text        = protoStream.readString()
      protoStream.readTag()
      val num         = protoStream.readInt32()
      MyObj(text, num)
    }
  }

  /// Represents a method on the API (aka. method on gRPC service)
  val method: MethodDescriptor[MyObj, MyObj] = MethodDescriptor
    .newBuilder()
    .setType(MethodDescriptor.MethodType.UNARY)
    // Method name with the namespace prefix
    .setFullMethodName("coop.rchain.Service/MyMethod")
    // Encoder/decoder for input and output types
    .setRequestMarshaller(myObjMarshal)
    .setResponseMarshaller(myObjMarshal)
    .build()

  /// An example how to execute client requests to the server (host) and respond
  def sendClientRequest(message: String): ManagedChannel = {
    // Creates channel object with the pointer to the server (host)
    val channel = NettyChannelBuilder
      .forAddress("localhost", 4321)
      .usePlaintext()
      .build

    // Creates new object that represents base gRPC API (to send/receive messages)
    val call = channel.newCall[MyObj, MyObj](method, CallOptions.DEFAULT)

    // Listener/callback to handle server responses
    val callListener = new ClientCall.Listener[MyObj] {
      override def onHeaders(headers: Metadata): Unit                =
        info(s"CLIENT_ON_HEADERS: $headers")
      override def onMessage(message: MyObj): Unit                   =
        info(s"CLIENT_ON_MESSAGE: $message")
      override def onClose(status: Status, trailers: Metadata): Unit =
        info(s"CLIENT_ON_CLOSE: ${status}, $trailers")
    }

    // Must be called first, starts the call (headers are sent here)
    call.start(callListener, /* headers = */ new Metadata())
    // Sends real network request (serialization to InputStream via Marshaller is done here)
    // Can be called multiple times which is "streaming" mode
    call.sendMessage(MyObj(message, 42))
    info(s"CLIENT_SENT_MSG: $message")
    // Number of messages to read next from the response (default is no read at all)
    // CHECK: Where is the buffer, client or server side
    call.request(1)
    // Clients marks no more messages with half-close
    call.halfClose()
    info(s"CLIENT_HALF_CLOSED")

    channel
  }

  /// An example how to start the server and listed for client requests and respond
  def startServer(port: Int): Server = {
    val serverCallHandler: ServerCallHandler[MyObj, MyObj] =
      new ServerCallHandler[MyObj, MyObj] {
        override def startCall(
          call: ServerCall[MyObj, MyObj],
          headers: Metadata,
        ): ServerCall.Listener[MyObj] = {
          info(s"SERVER_START_CALL: ${call.getMethodDescriptor}")
          info(s"SERVER_START_CALL: $headers")

          // Number of messages to read next from the response (default is no read at all)
          call.request(1)

          // Server listener of clients requests
          new ServerCall.Listener[MyObj] {
            // Handle client request message and optionally respond
            override def onMessage(message: MyObj): Unit = {
              info(s"SERVER_ON_MESSAGE: $message")
              // Sends headers
              call.sendHeaders(headers)
              // Sends the message
              call.sendMessage(MyObj(s"Server responds to: $message", 42))
              info(s"SERVER_SENT_RESPOND_MSG")
              // Close must be called, but only once to prevent exception
              call.close(Status.OK, headers)

              // An example of server error
              // call.close(Status.UNKNOWN.withDescription("Custom server error"), new Metadata())
            }

            override def onHalfClose(): Unit = info(s"SERVER_ON_HALF_CLOSE")
            override def onCancel(): Unit    = info(s"SERVER_ON_CANCEL")
            override def onComplete(): Unit  = info(s"SERVER_ON_COMPLETE")
          }
        }
      }

    // Service (API) description, mapping of methods specification with handlers
    val serviceDef = ServerServiceDefinition
      .builder("coop.rchain.Service")
      .addMethod(method, serverCallHandler)
      .build()

    // Bind listener to the network adapter accepting API schema (service definition)
    NettyServerBuilder
      .forPort(port)
      .addService(serviceDef)
      .build
      .start()
  }

  def info(msg: Any): Unit = {
    val date = LocalDateTime.now()
    val time = date.toLocalTime
    println(s"[$time] $msg")
  }

  "grpc server & client" should "be defined with low level API directly" in {
    val server = startServer(4321)

    val channel = sendClientRequest("Hello from client!")

    Thread.sleep(250)
    channel.shutdown()
    server.shutdown()
  }
}
