package node

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import cats.syntax.all.*
import dproc.data.Block
import node.rpc.syntax.all.grpcClientSyntax
import node.rpc.{GrpcChannelsManager, GrpcClient, GrpcServer}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sdk.data.{BalancesDeploy, HostWithPort}
import sdk.primitive.ByteArray

class GrpcDslCommSpec extends AnyFlatSpec with Matchers {

  "Grpc server" should "correctly handle all comm protocol defined." in {
    val serverPort   = 4321
    val serverHost   = "localhost"
    val serverSocket = HostWithPort(serverHost, serverPort)

    val srcMessage       = ByteArray(Array[Byte](1))
    val expectedResponse = false // false since true is default

    val grpcServer = GrpcServer.apply[IO](
      serverPort,
      (_, _) => expectedResponse.pure[IO],
      _ => none[Block.WithId[ByteArray, ByteArray, BalancesDeploy]].pure[IO],
      _ => List.empty[ByteArray].pure[IO],
    )

    val grpcCall = GrpcChannelsManager[IO].use { implicit ch =>
      GrpcClient[IO].reportBlockHash(srcMessage, HostWithPort("", 123), serverSocket)
    }

    grpcServer.use(_ => grpcCall.map(resp => resp shouldBe expectedResponse)).unsafeRunSync()
  }
}
