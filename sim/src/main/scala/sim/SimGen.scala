package sim

import cats.effect.{ExitCode, IO, IOApp}
import io.circe.{Json, Printer}
import io.circe.syntax.EncoderOps
import sdk.data.BalancesState
import sdk.hashing.Blake2b
import sdk.primitive.ByteArray
import sdk.syntax.all.sdkSyntaxByteArray
import weaver.data.{Bonds, FinalData}

object SimGen extends IOApp {
  def generate(netSize: Int, usersNum: Int): (FinalData[ByteArray], BalancesState) = {
    val rnd                   = new scala.util.Random()
    /// Users (wallets) making transactions
    val users: Set[ByteArray] =
      (1 to usersNum).map(_ => Array(rnd.nextInt().toByte)).map(Blake2b.hash256).map(ByteArray(_)).toSet
    val genesisPoS            = {
      val rnd               = new scala.util.Random()
      /// Genesis data
      val lazinessTolerance = 1 // c.lazinessTolerance
      val senders           =
        Iterator
          .range(0, netSize)
          .map(_ => Array(rnd.nextInt().toByte))
          .map(Blake2b.hash256)
          .map(ByteArray(_))
          .toSet
      // Create lfs message, it has no parents, sees no offences and final fringe is empty set
      val genesisBonds      = Bonds(senders.map(_ -> 100L).toMap)
      FinalData(genesisBonds, lazinessTolerance, 10000)
    }
    val genesisBalances       = new BalancesState(users.map(_ -> Long.MaxValue / 2).toMap)

    genesisPoS -> genesisBalances
  }

  override def run(args: List[String]): IO[ExitCode] = {
    val netSize                       = args.headOption.map(_.toInt).getOrElse(10)
    val usersNum                      = args.lift(1).map(_.toInt).getOrElse(100)
    val (genesisPoS, genesisBalances) = generate(netSize, usersNum)
    val posJson                       = genesisPoS.bonds.bonds.map { case (k, v) => k.toHex -> v }.asJson
    val walletsJson                   = genesisBalances.diffs.map { case (k, v) => k.toHex -> v }.asJson
    println(
      s"""Genesis PoS (put into pos.json):
         |${posJson.printWith(Printer.noSpaces)}
         |
         |Genesis wallets (put into wallets.json):
         |${walletsJson.printWith(Printer.noSpaces)}
         |""".stripMargin,
    )

    IO(ExitCode.Success)
  }
}
