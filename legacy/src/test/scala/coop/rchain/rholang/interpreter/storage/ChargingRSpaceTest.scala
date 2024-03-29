package coop.rchain.rholang.interpreter.storage

import cats.effect.unsafe.implicits.global
import cats.effect.{IO, Sync}
import com.google.protobuf.ByteString
import coop.rchain.crypto.hash.Blake2b512Random
import coop.rchain.metrics
import coop.rchain.metrics.{Metrics, NoopSpan}
import coop.rchain.models.Expr.ExprInstance.GInt
import coop.rchain.models.TaggedContinuation.TaggedCont.ParBody
import coop.rchain.models.Var.VarInstance.FreeVar
import coop.rchain.models._
import coop.rchain.models.rholang.implicits._
import coop.rchain.rholang.Resources.mkRhoISpace
import coop.rchain.rholang.interpreter.RhoRuntime.{RhoISpace, RhoTuplespace}
import coop.rchain.rholang.interpreter.accounting
import coop.rchain.rholang.interpreter.accounting.CostAccounting.CostStateRef
import coop.rchain.rholang.interpreter.accounting._
import coop.rchain.rholang.interpreter.errors.OutOfPhlogistonsError
import coop.rchain.rholang.interpreter.storage.ChargingRSpaceTest.{ChargingRSpace, _}
import coop.rchain.shared.Log
import coop.rchain.shared.scalatestcontrib.AnyShouldF
import coop.rchain.store.InMemoryStoreManager
import org.scalactic.TripleEqualsSupport
import org.scalatest.Outcome
import org.scalatest.flatspec.FixtureAnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration._

class ChargingRSpaceTest extends FixtureAnyFlatSpec with TripleEqualsSupport with Matchers {

  behavior of "ChargingRSpace"

  val channels                = channelsN(1)
  val patterns                = patternsN(1)
  val cont                    = continuation()
  val consumeStorageCost      = accounting.storageCostConsume(channels, patterns, cont)
  val channel                 = channels.head
  val data                    = NilPar
  val produceStorageCost      = accounting.storageCostProduce(channel, data)
  val produceEventStorageCost = accounting.eventStorageCost(1)
  val consumeEventStorageCost = accounting.eventStorageCost(channels.size)
  val commEventStorageCost    = accounting.commEventStorageCost(channels.size)

  it should "charge for storing data in tuplespace" in { fixture =>
    val TestFixture(chargingRSpace, cost) = fixture
    val minimumPhlos                      = consumeStorageCost + consumeEventStorageCost

    val test = for {
      _ <- cost.set(minimumPhlos)
      _ <- chargingRSpace.consume(channels, patterns, cont, false)
      _ <- cost.current shouldBeF Cost(0)
    } yield ()

    test.timeout(1.second).unsafeRunSync()
  }

  it should "refund if data doesn't stay in tuplespace" in { fixture =>
    val TestFixture(chargingRSpace, cost) = fixture
    val minimumPhlos =
      produceStorageCost + produceEventStorageCost + consumeStorageCost + consumeEventStorageCost + commEventStorageCost

    val test = for {
      _ <- cost.set(minimumPhlos)
      _ <- chargingRSpace.produce(channels.head, data, false)
      _ <- cost.current shouldBeF (minimumPhlos - produceStorageCost - produceEventStorageCost)
      _ <- chargingRSpace.consume(channels, patterns, cont, false)
      _ <- cost.current shouldBeF (consumeStorageCost + produceStorageCost)
    } yield ()

    test.timeout(1.second).unsafeRunSync()
  }

  it should "fail with OutOfPhloError when deploy runs out of it" in { fixture =>
    val TestFixture(chargingRSpace, cost) = fixture

    val test = for {
      _ <- cost.set(produceStorageCost - Cost(1))
      _ <- chargingRSpace.produce(channel, data, false)
    } yield ()

    val outOfPhloTest = test.attempt.unsafeRunSync()
    assert(outOfPhloTest === Left(OutOfPhlogistonsError))

    val costTest = cost.current.unsafeRunSync()
    assert(costTest.value === -1)
  }

  it should "charge COMM on a join properly when parts of the join are deployed separately" in {
    // first deploy:
    // for(x <- @x & y <- @y) { P }
    // second deploy:
    // @x!(data)
    // third deploy:
    // @y!(data)
    // last deployment should be refunded with the cost of storing two previous deployments
    fixture =>
      val TestFixture(chargingRSpace, cost) = fixture
      val channels                          = channelsN(2)
      val patterns                          = patternsN(2)
      val firstProdCost                     = accounting.storageCostProduce(channels(0), data)
      val secondProdCost                    = accounting.storageCostProduce(channels(1), data)
      val joinCost                          = accounting.storageCostConsume(channels, patterns, cont)
      val consumeEventStorageCost           = accounting.eventStorageCost(channels.size)
      val commEventStorageCost              = accounting.commEventStorageCost(channels.size)

      val minimumPhlos =
        firstProdCost + produceEventStorageCost +
          secondProdCost + produceEventStorageCost +
          joinCost + consumeEventStorageCost + commEventStorageCost

      val test = for {
        _                   <- cost.set(minimumPhlos)
        _                   <- chargingRSpace.consume(channels, patterns, cont, false)
        phlosAfterConsume   <- cost.current
        _                   = phlosAfterConsume shouldBe (minimumPhlos - consumeEventStorageCost - joinCost)
        _                   <- chargingRSpace.produce(channels(0), data, false)
        phlosAfterFirstSend <- cost.current
        _                   = phlosAfterFirstSend shouldBe (phlosAfterConsume - firstProdCost - produceEventStorageCost)
        _                   <- chargingRSpace.produce(channels(1), data, false)
        phlosLeft           <- cost.current
        _                   = phlosLeft.value shouldBe (firstProdCost + secondProdCost + joinCost).value
      } yield ()

      test.timeout(1.second).unsafeRunSync()
  }

  it should "not charge for storage if linear terms create a COMM" in { fixture =>
    // for(x <- @x) | @x!(10)
    // we should not charge for storing any of the terms
    val TestFixture(chargingRSpace, cost) = fixture

    val data               = ListParWithRandom().withPars(Vector(GInt(1)))
    val produceStorageCost = storageCostProduce(channel, data)

    val initPhlos =
      consumeStorageCost + consumeEventStorageCost + produceStorageCost + produceEventStorageCost + commEventStorageCost

    val test = for {
      _ <- cost.set(initPhlos)
      _ <- chargingRSpace.consume(channels, patterns, cont, false)
      _ <- chargingRSpace.produce(channel, data, false)
      _ <- cost.current shouldBeF (consumeStorageCost + produceStorageCost)
    } yield ()

    test.timeout(1.second).unsafeRunSync()
  }

  it should "charge for storing persistent produce that create a COMM" in { fixture =>
    // for(x <- @x) { P } | @x!!(100)
    // we should charge for storing non-linear produce
    val TestFixture(chargingRSpace, cost) = fixture
    val pattern                           = BindPattern(Vector(EVar(FreeVar(0))))

    val data        = ListParWithRandom().withPars(Vector(GInt(1)))
    val produceCost = storageCostProduce(channel, data)

    val initPhlos = Cost(1000)

    val test = for {
      _ <- cost.set(initPhlos)
      _ <- chargingRSpace.produce(channel, data, true)
      _ <- chargingRSpace.consume(channels, List(pattern), cont, false)
      _ <- cost.current shouldBeF (
            initPhlos - produceCost - produceEventStorageCost - consumeEventStorageCost - commEventStorageCost
          )
    } yield ()

    test.timeout(1.second).unsafeRunSync()
  }

  it should "charge for storing persistent consume that create a COMM" in { fixture =>
    // for(x <= @x) { P } | @x!(100)
    // we should charge for storing non-linear continuation
    val TestFixture(chargingRSpace, cost) = fixture

    val data = ListParWithRandom().withPars(Vector(GInt(1)))

    val initPhlos = Cost(1000)

    val test = for {
      _ <- cost.set(initPhlos)
      _ <- chargingRSpace.consume(channels, patterns, cont, true)
      _ <- chargingRSpace.produce(channel, data, false)
      _ <- cost.current shouldBeF (
            initPhlos - consumeStorageCost - produceEventStorageCost - consumeEventStorageCost - commEventStorageCost
          )
    } yield ()

    test.timeout(1.second).unsafeRunSync()
  }

  it should "refund for linear data in join" in { fixture =>
    // idea for the test is that we have persistent and non persistent produce in first deploy:
    // @"x"!!(1) | @"y"!(10)
    // and consume on joined channels in another:
    // for(x <- @"x" & y <- @"y") { … }
    // In this case we shouldn't charge for storing consume and refund for removing produce on @"y"

    val TestFixture(chargingRSpace, cost) = fixture
    val channels                          = channelsN(2)
    val patterns                          = patternsN(2)
    val cont                              = continuation()

    val dataX = ListParWithRandom().withPars(Vector(GInt(1)))
    val dataY = ListParWithRandom().withPars(Vector(GInt(10)))

    val produceYCost            = accounting.storageCostProduce(channels(1), dataY)
    val consumeEventStorageCost = accounting.eventStorageCost(channels.size)
    val commEventStorageCost    = accounting.commEventStorageCost(channels.size)

    val initPhlos = Cost(1000)

    val test = for {
      _ <- cost.set(initPhlos)
      _ <- chargingRSpace.produce(channels(0), dataX, persist = true)
      _ <- chargingRSpace.produce(channels(1), dataY, persist = false)
      _ <- cost.set(initPhlos)
      _ <- chargingRSpace.consume(channels, patterns, cont, false)
      _ <- cost.current shouldBeF (initPhlos + produceYCost - consumeEventStorageCost - commEventStorageCost)
    } yield ()

    test.timeout(1.second).unsafeRunSync()
  }

  it should "refund for removing consume" in { fixture =>
    // first deploy:
    // for(x <- @x) { P }
    // second deploy:
    // @x!(100)
    // we should refund for removing continuation from tuplespace
    val TestFixture(chargingRSpace, cost) = fixture

    val initPhlos = Cost(1000)

    val test = for {
      _ <- cost.set(initPhlos)
      _ <- chargingRSpace.consume(channels, patterns, cont, false)
      _ <- cost.set(initPhlos)
      _ <- chargingRSpace.produce(channel, data, persist = false)
      _ <- cost.current shouldBeF (initPhlos + consumeStorageCost - produceEventStorageCost - commEventStorageCost)
    } yield ()

    test.timeout(1.second).unsafeRunSync()
  }

  it should "refund for removing produce" in { fixture =>
    // first deploy:
    // @x!(100)
    // second deploy:
    // for(x <- @x) { P }
    // we should refund for removing @x!(100) from tuplespace
    val TestFixture(chargingRSpace, cost) = fixture

    val data        = ListParWithRandom().withPars(Vector(GInt(1)))
    val produceCost = accounting.storageCostProduce(channel, data)

    val initPhlos = Cost(1000)

    val test = for {
      _ <- cost.set(initPhlos)
      _ <- chargingRSpace.produce(channel, data, persist = false)
      _ <- cost.set(initPhlos)
      _ <- chargingRSpace.consume(channels, patterns, cont, false)
      _ <- cost.current shouldBeF (initPhlos + produceCost - consumeEventStorageCost - commEventStorageCost)
    } yield ()

    test.timeout(1.second).unsafeRunSync()
  }

  it should "refund for clearing tuplespace" in { fixture =>
    // first deploy:
    // @x!(100) | @y!(10) | for(x <- @x & y <- @y & z <- @z) { P }
    // second deploy:
    // @z!(1)
    // since second deploy triggers continuation we should refund with the cost of storing first deploy
    val TestFixture(chargingRSpace, cost) = fixture
    val List(x, y, z)                     = channelsN(3)
    val patterns                          = patternsN(3)
    val cont                              = continuation()

    val dataX                = ListParWithRandom().withPars(Vector(GInt(1)))
    val dataY                = ListParWithRandom().withPars(Vector(GInt(10)))
    val dataZ                = ListParWithRandom().withPars(Vector(GInt(100)))
    val produceXCost         = accounting.storageCostProduce(x, dataX)
    val produceYCost         = accounting.storageCostProduce(y, dataY)
    val consumeCost          = accounting.storageCostConsume(List(x, y, z), patterns, cont)
    val commEventStorageCost = accounting.commEventStorageCost(3)

    val initPhlos = Cost(10000)

    val test = for {
      _ <- cost.set(initPhlos)
      _ <- chargingRSpace.produce(x, dataX, false)
      _ <- chargingRSpace.produce(y, dataY, false)
      _ <- chargingRSpace.consume(List(x, y, z), patterns, cont, false)
      _ <- cost.set(initPhlos)
      _ <- chargingRSpace.produce(z, dataZ, false)
      _ <- cost.current shouldBeF (
            initPhlos + produceXCost + produceYCost + consumeCost - produceEventStorageCost - commEventStorageCost
          )
    } yield ()

    test.timeout(5.second).unsafeRunSync()
  }

  override type FixtureParam = TestFixture

  protected override def withFixture(test: OneArgTest): Outcome = {
    val cost: CostStateRef[IO] = CostAccounting.emptyCost[IO].unsafeRunSync()
    implicit val span          = NoopSpan[IO]()
    implicit val kvm           = InMemoryStoreManager[IO]()

    def mkChargingRspace(rhoISpace: RhoISpace[IO]): IO[ChargingRSpace] = {
      val s = implicitly[Sync[IO]]
      IO.delay(ChargingRSpace.chargingRSpace(rhoISpace)(s, cost, span))
    }

    mkRhoISpace[IO]
      .flatMap(mkChargingRspace)
      .flatMap(chargingRSpace => IO.delay { test(TestFixture(chargingRSpace, cost)) })
      .timeout(10.seconds)
      .unsafeRunSync()
  }

}

object ChargingRSpaceTest {
  type ChargingRSpace = RhoTuplespace[IO]
  final case class TestFixture(chargingRSpace: ChargingRSpace, cost: CostStateRef[IO])

  val NilPar = ListParWithRandom().withPars(Seq(Par()))

  def channelsN(n: Int): List[Par] =
    (1 to n).map(x => byteName(x.toByte)).toList

  private def byteName(b: Byte): Par = GPrivate(ByteString.copyFrom(Array[Byte](b)))

  def patternsN(n: Int): List[BindPattern] =
    (1 to n)
      .map(
        _ => BindPattern(Vector(EVar(Var(FreeVar(0)))), freeCount = 1)
      )
      .toList

  def continuation(
      par: Par = Par().withExprs(Seq(GInt(1))),
      r: Blake2b512Random = Blake2b512Random.defaultRandom
  ): TaggedContinuation =
    TaggedContinuation(ParBody(ParWithRandom(par, r)))

  implicit val logF: Log[IO]            = new Log.NOPLog[IO]
  implicit val noopMetrics: Metrics[IO] = new metrics.Metrics.MetricsNOP[IO]
  implicit val ms: Metrics.Source       = Metrics.BaseSource
}
