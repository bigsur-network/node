import api.Servers
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import cats.syntax.all.*
import io.grpc.*
import io.grpc.netty.NettyChannelBuilder
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime
import rhonix.api.grpc.methods.Balances
import sdk.api.FindApi

import scala.util.Random

class GrpcDslSpec extends AnyFlatSpec with Matchers {
  val serverPort = 4321

  /// An example how to execute client requests to the server (host) and respond
  def callServer(wallet: String, setResult: Long => Unit): Unit = {
    // Creates channel object with the pointer to the server (host)
    val channel = NettyChannelBuilder
      .forAddress("localhost", serverPort)
      .usePlaintext()
      .build

    // Creates new object that represents base gRPC API (to send/receive messages)
    val call =
      channel.newCall[Balances.Request, Balances.Response](Balances.balancesMethodDescriptor, CallOptions.DEFAULT)

    // Listener/callback to handle server responses
    val callListener = new ClientCall.Listener[Balances.Response] {
      override def onHeaders(headers: Metadata): Unit =
        info(s"CLIENT_ON_HEADERS: $headers")

      override def onMessage(message: Balances.Response): Unit = {
        setResult(message.balance)
        info(s"CLIENT_ON_MESSAGE: $message")
      }

      override def onClose(status: Status, trailers: Metadata): Unit =
        info(s"CLIENT_ON_CLOSE: ${status}, $trailers")
    }

    // Must be called first, starts the call (headers are sent here)
    call.start(callListener, /* headers = */ new Metadata())
    // Sends real network request (serialization to InputStream via Marshaller is done here)
    // Can be called multiple times which is "streaming" mode
    call.sendMessage(Balances.Request("wallet"))
    info(s"CLIENT_SENT_MSG: $wallet")
    // Number of messages to read next from the response (default is no read at all)
    // CHECK: Where is the buffer, client or server side
    call.request(1)
    // Clients marks no more messages with half-close
    call.halfClose()
    info(s"CLIENT_HALF_CLOSED")
    channel.shutdown()
    ()
  }

  "grpc server & client" should "be defined with low level API directly" in {
    var result            = Long.MaxValue
    val referenceResponse = Random.nextLong()

    val balancesReader = new FindApi[IO, String, Long] {
      override def find[R](id: String, proj: Long => R): IO[Option[R]] = proj(referenceResponse).some.pure[IO]

      override def findAll(proj: (String, Long) => Boolean): fs2.Stream[IO, (String, Long)] = fs2.Stream.empty
    }

    val clientCall = IO.delay(callServer("doesNotMatter", result = _)) <* IO.sleep(250.millis)

    Servers.rpc[IO](serverPort, balancesReader).surround(clientCall).unsafeRunSync()

    result shouldBe referenceResponse
  }
}
