package slick

import cats.effect.unsafe.implicits.global
import cats.effect.{Async, IO, Resource, Sync}
import cats.syntax.all.*
import org.scalacheck.ScalacheckShapeless.derivedArbitrary
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import sdk.comm.Peer
import sdk.data.{Block, Deploy}
import sdk.primitive.ByteArray
import slick.SlickSpec.*
import slick.api.SlickApi
import slick.jdbc.PostgresProfile
import slick.migration.api.PostgresDialect
import slick.syntax.all.*

class SlickSpec extends AsyncFlatSpec with Matchers with ScalaCheckPropertyChecks {

  "deployInsert() function call" should "add the correct entry to the Deploys, Deployers and Shards table" in {
    forAll { (d: Deploy) =>
      embedPgSlick[IO]
        .use { api =>
          for {
            _       <- api.deployInsert(d)
            dFromDB <- api.deployGet(d.sig)
          } yield d shouldBe dFromDB.get
        }
        .unsafeRunSync()
    }
  }

  "deployInsert() function call" should "not duplicate records in the Deployers and Shards tables if they are the same" in {
    forAll { (d1: Deploy, d2Sig: ByteArray) =>
      val d2: Deploy = d1.copy(sig = d2Sig) // create d2 with the same fields but another sig
      embedPgSlick[IO]
        .use { api =>
          for {
            _        <- api.deployInsert(d1)
            _        <- api.deployInsert(d2)
            d1FromDB <- api.deployGet(d1.sig)
            d2FromDB <- api.deployGet(d2.sig)
          } yield {
            Some(d1) shouldBe d1FromDB
            Some(d2) shouldBe d2FromDB
          }
        }
        .unsafeRunSync()
    }
  }

  "Concurrent deployInsert() function calls" should "correctly handle concurrency" in {
    // Test inserting the same deploy 100 times in parallel.
    // During insertion all nested fields are first saved if not present (e.g. deploy id, shard id)

    val d       = Arbitrary.arbitrary[Deploy].sample.get
    val sigs    = Gen.listOfN(100, Arbitrary.arbitrary[ByteArray]).sample.get
    val deploys = sigs.map(s => d.copy(sig = s))

    embedPgSlick[IO]
      .use { api =>
        deploys.parTraverse_(api.deployInsert).map(_ shouldBe an[Unit]) >>
          sigs.traverse(api.deployGet).map(_.count(_.isDefined)).map(_ shouldBe sigs.length)
      }
      .unsafeRunSync()
  }

  "blockInsert() function call" should "add the correct entry to the Blocks table and to the all related tables" in {
    forAll(
      Gen.listOfN(3, Arbitrary.arbitrary[ByteArray]),
      nonEmptyDeploySeqGen,
      nonEmptyBondsMapGen,
      Arbitrary.arbitrary[Block],
      Arbitrary.arbitrary[Block],
      nonEmptyAlphaString,
    ) { (hashes, deploys_, bMap, b1_, b2_, shardName) =>
      val dSetHash = hashes.get(0).get
      val bMapHash = hashes.get(1).get
      val bSetHash = hashes.get(2).get

      val b2Hash  = Arbitrary.arbitrary[ByteArray].suchThat(_ != b1_.hash).sample.get
      val b1      = b1_.copy(shardName = shardName, sigAlg = nonEmptyAlphaString.sample.get)
      val b2      = b2_.copy(hash = b2Hash, shardName = shardName, sigAlg = nonEmptyAlphaString.sample.get)
      val deploys = deploys_.map(_.copy(shardName = shardName, program = nonEmptyAlphaString.sample.get))

      embedPgSlick[IO]
        .use { api =>
          for {
            _             <- deploys.toSeq.traverse(api.deployInsert)
            dSetSigs       = deploys.map(_.sig)
            insertedBlock1 = sdk.data.Block(
                               version = b1.version,
                               hash = b1.hash,
                               sigAlg = b1.sigAlg,
                               signature = b1.signature,
                               finalStateHash = b1.finalStateHash,
                               postStateHash = b1.postStateHash,
                               validatorPk = b1.validatorPk,
                               shardName = b1.shardName,
                               justificationSet = Set(),
                               seqNum = b1.seqNum,
                               offencesSet = Set(),
                               bondsMap = bMap,
                               finalFringe = Set(),
                               execDeploySet = dSetSigs,
                               mergeDeploySet = dSetSigs,
                               dropDeploySet = Set(),
                               mergeDeploySetFinal = Set(),
                               dropDeploySetFinal = Set(),
                             )
            _             <- api.blockInsert(insertedBlock1)(
                               ByteArray.Default,
                               ByteArray.Default,
                               bMapHash,
                               ByteArray.Default,
                               dSetHash,
                               dSetHash,
                               ByteArray.Default,
                               ByteArray.Default,
                               ByteArray.Default,
                             )

            insertedBlock2 = sdk.data.Block(
                               version = b2.version,
                               hash = b2.hash,
                               sigAlg = b2.sigAlg,
                               signature = b2.signature,
                               finalStateHash = b2.finalStateHash,
                               postStateHash = b2.postStateHash,
                               validatorPk = b2.validatorPk,
                               shardName = b2.shardName,
                               justificationSet = Set(b1.hash),
                               seqNum = b2.seqNum,
                               offencesSet = Set(b1.hash),
                               bondsMap = bMap,
                               finalFringe = Set(b1.hash),
                               execDeploySet = Set(),
                               mergeDeploySet = Set(),
                               dropDeploySet = dSetSigs,
                               mergeDeploySetFinal = dSetSigs,
                               dropDeploySetFinal = dSetSigs,
                             )
            _             <- api.blockInsert(insertedBlock2)(
                               bSetHash,
                               bSetHash,
                               bMapHash,
                               bSetHash,
                               ByteArray.Default,
                               ByteArray.Default,
                               dSetHash,
                               dSetHash,
                               dSetHash,
                             )

            readBlock1 <- api.blockGet(b1.hash)
            readBlock2 <- api.blockGet(b2.hash)
          } yield {
            readBlock1 shouldBe Some(insertedBlock1)
            readBlock2 shouldBe Some(insertedBlock2)
          }
        }
        .unsafeRunSync()
    }
  }

  "Stored and loaded name-value pairs" should "be the same" in {
    embedPgSlick[IO]
      .use { api =>
        implicit val slickDb: SlickDb         = api.slickDb
        import org.scalacheck.Shrink
        implicit val noShrink: Shrink[String] = Shrink.shrinkAny

        IO.delay {
          forAll(nonEmptyAlphaString, nonEmptyAlphaString) { (name, value) =>
            val storeF = api.actions.putConfig(name, value).run[IO]
            val loadF  = api.actions.getConfig(name).run[IO]
            (storeF >> loadF).unsafeRunSync() shouldBe Some(value)
          }
        }
      }
      .unsafeRunSync()
  }

  "Loaded peers" should "be the same as generated and stored peers" in {
    forAll(Gen.nonEmptyListOf(nonEmptyAlphaString)) { urls =>
      val peers = urls.distinct.map(Peer(_, port = 1234, isSelf = false, isValidator = true)) match {
        case head :: tail => head.copy(isSelf = true) +: tail
        case list         => list
      }

    embedPgSlick[IO]
      .use { api =>
        implicit val async            = Async[IO]
        implicit val slickDb: SlickDb = api.slickDb

        for {
          _           <-
            peers.traverse(peer => api.actions.peerInsertIfNot(peer.host, peer.port, peer.isSelf, peer.isValidator).run)
          loadedPeers <- api.actions.peers.run
        } yield peers shouldBe loadedPeers
      }
      .unsafeRunSync()
    }
  }

  "Insert and then remove peer" should "lead to empty peer table" in {
    embedPgSlick[IO]
      .use { api =>
        implicit val async            = Async[IO]
        implicit val slickDb: SlickDb = api.slickDb

        val host = "host"
        val port = 1234
        val peer = Peer(host, port, isSelf = true, isValidator = true)
        for {
          _       <- api.actions.peerInsertIfNot(peer.host, peer.port, peer.isSelf, peer.isValidator).run
          _       <- api.actions.removePeer(peer.host).run
          dbPeers <- api.actions.peers.run
        } yield dbPeers shouldBe Seq.empty[Peer]
      }
      .unsafeRunSync()
  }
}

object SlickSpec {

  def embedPgSlick[F[_]: Async]: Resource[F, SlickApi[F]] =
    EmbeddedPgSqlSlickDb[F].evalMap(x => SlickDb(x, PostgresProfile, new PostgresDialect).flatMap(SlickApi[F]))

  // Define Arbitrary for ByteArray since it's a custom type and needs specific generation logic
  implicit val byteArrayArbitrary: Arbitrary[ByteArray] = Arbitrary {
    Gen.nonEmptyListOf(Gen.alphaChar).map(chars => ByteArray(chars.mkString.getBytes))
  }

  val nonEmptyDeploySeqGen: Gen[Set[Deploy]] = for {
    size    <- Gen.chooseNum(1, 10) // Choose a suitable max value
    deploys <- Gen.listOfN(size, Arbitrary.arbitrary[Deploy])
  } yield deploys.toSet

  val nonEmptyBondsMapGen: Gen[Map[ByteArray, Long]] = for {
    size  <- Gen.chooseNum(1, 10) // Choose a suitable max value
    bonds <- Gen.listOfN(size, Arbitrary.arbitrary[(ByteArray, Long)])
  } yield bonds.toMap

  val nonEmptyAlphaString: Gen[String] = Gen.nonEmptyListOf(Gen.alphaChar).map(_.mkString)
}
