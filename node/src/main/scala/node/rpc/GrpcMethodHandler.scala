package node.rpc

import cats.effect.std.Dispatcher
import io.grpc.{Metadata, ServerCall, ServerCallHandler, Status}
import sdk.log.Logger.*

object GrpcMethodHandler {
  def apply[F[_], Req, Resp](
    callback: Req => F[Resp], // request and remote address
    dispatcher: Dispatcher[F],
  ): ServerCallHandler[Req, Resp] = new ServerCallHandler[Req, Resp] {
    override def startCall(
      call: ServerCall[Req, Resp],
      headers: Metadata,
    ): ServerCall.Listener[Req] = {
      logDebug(s"SERVER_START_CALL: ${call.getMethodDescriptor}")
      logDebug(s"SERVER_START_CALL: $headers")

      // Number of messages to read next from the response (default is no read at all)
      call.request(1)

      new ServerCall.Listener[Req] {
        override def onMessage(message: Req): Unit = {
          logDebug(s"SERVER_ON_MESSAGE: $message")
          call.sendHeaders(headers)
          val result = dispatcher.unsafeRunSync(callback(message))
          call.sendMessage(result)
          logDebug(s"SERVER_SENT_RESPOND_MSG $result")
          call.close(Status.OK, headers)
        }

        override def onHalfClose(): Unit = logDebug(s"SERVER_ON_HALF_CLOSE")

        override def onCancel(): Unit = logDebug(s"SERVER_ON_CANCEL")

        override def onComplete(): Unit = logDebug(s"SERVER_ON_COMPLETE")
      }
    }
  }

}
