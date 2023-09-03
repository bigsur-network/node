package db

import cats.data.OptionT
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.scalacheck.ScalacheckShapeless.*
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import sdk.api.data.*
import sdk.db.*
import squeryl.api.*

// TODO re-enable these tests when it is possible to execute them on CI.
// At the moment Github Actions does not allow non-root users and embedded postgres requires non-root.
// https://github.com/fergusstrange/embedded-postgres/issues/95
class DbSpec extends AsyncFlatSpec with Matchers with ScalaCheckPropertyChecks {

  "Validator insert function call" should "add the correct entry to the Validator table" ignore {
    forAll { (validator: Validator) =>
      def test(api: ValidatorDbApiImpl[IO]) = for {
        id                <- api.insert(validator)
        validatorById     <- OptionT(api.getById(id)).getOrRaise(new RecordNotFound)
        validatorByPubKey <- OptionT(api.getByPublicKey(validator.publicKey)).getOrRaise(new RecordNotFound)
      } yield {
        id shouldBe 1L

        validatorById.publicKey shouldBe validator.publicKey
        validatorById.http shouldBe validator.http

        validatorById shouldBe validatorByPubKey
      }

      LiquiPgSql[IO].map(implicit c => new ValidatorDbApiImpl[IO]).use(test).unsafeRunSync()
    }
  }

  "Bond insert function call" should "add the correct entry to the Bond table" ignore {
    forAll { (validator: Validator, bondStake: Long) =>
      def test(validatorAip: ValidatorDbApiImpl[IO], bondApi: BondDbApiImpl[IO]) = for {
        validatorId <- validatorAip.insert(validator)
        _           <- OptionT[IO, Validator](validatorAip.getById(validatorId)).getOrRaise(new RecordNotFound)
        bondId      <- bondApi.insert(Bond(validator, bondStake), validatorId)
        bond        <- OptionT(bondApi.getById(bondId)).getOrRaise(new RecordNotFound)
      } yield {
        bondId shouldBe 1L
        bond.stake shouldBe bondStake
      }
      LiquiPgSql[IO]
        .map(implicit c => (new ValidatorDbApiImpl[IO], new BondDbApiImpl[IO]))
        .use { case (vapi, bapi) => test(vapi, bapi) }
        .unsafeRunSync()
    }
  }

  "Deploy insert function call" should "add the correct entry to the Deploy table" ignore {
    forAll { (deploy: Deploy) =>
      def test(api: DeployDbApiImpl[IO]) = for {
        deployId     <- api.insert(deploy)
        deployById   <- OptionT(api.getById(deployId)).getOrRaise(new RecordNotFound)
        deployByHash <- OptionT(api.getByHash(deploy.hash)).getOrRaise(new RecordNotFound)
      } yield {
        deployId shouldBe 1L
        deploy shouldBe deployById
        deployById shouldBe deployByHash
      }

    LiquiPgSql[IO].map(implicit c => new DeployDbApiImpl[IO]).use(test).unsafeRunSync()
    }
  }

  "Block insert function call" should "add the correct entry to the Block table" ignore {
    forAll { (validator: Validator, block: Block) =>
      def test(blockDbApi: BlockDbApiImpl[IO]) = for {
        // Correction of the generated data
        blockUpdated <- IO.delay(
                          block.copy(
                            sender = validator,
                            bonds = Set.empty[Bond],
                            deploys = Set.empty[Deploy],
                            justifications = Set.empty[Validator],
                          ),
                        )
        // Inserting data into DB
        validatorId  <- blockDbApi.validatorDbApi.insert(validator)
        blockId      <- blockDbApi.insert(blockUpdated, validatorId)

        // Getting data from DB
        blockById   <- OptionT(blockDbApi.getById(blockId)).getOrRaise(new RecordNotFound)
        blockByHash <- OptionT(blockDbApi.getByHash(block.hash)).getOrRaise(new RecordNotFound)
      } yield {
        blockId shouldBe 1L
        blockById shouldBe blockUpdated
        blockByHash shouldBe blockUpdated
      }

      LiquiPgSql[IO]
        .map { implicit c =>
          BlockDbApiImpl(
            new ValidatorDbApiImpl[IO],
            new BlockJustificationsDbApiImpl[IO],
            new BlockBondsDbApiImpl[IO],
            new BlockDeploysDbApiImpl[IO],
            new BondDbApiImpl[IO],
            new DeployDbApiImpl[IO],
          )
        }
        .use(test)
        .unsafeRunSync()
    }
  }

  "Complex Db logic" should "works correct" ignore {
    forAll {
      (validator1: Validator, validator2: Validator, bond: Bond, deploy1: Deploy, deploy2: Deploy, block: Block) =>
        def test(blockDbApi: BlockDbApiImpl[IO]) = for {
          _                <- IO.delay(())
          // Correction of the generated data

          // publicKey should be unique
          validator1Updated = validator1.copy(publicKey = Array[Byte](1))
          validator2Updated = validator2.copy(publicKey = Array[Byte](2))

          // deploy hash should be unique
          deploy1Updated = deploy1.copy(hash = Array[Byte](1))
          deploy2Updated = deploy2.copy(hash = Array[Byte](2))

          bondUpdated   = bond.copy(validator = validator1Updated)
          blockUpdated  = block.copy(
                            sender = validator1Updated,
                            justifications = Set(validator1Updated, validator2Updated),
                            bonds = Set(bondUpdated),
                            deploys = Set(deploy1Updated, deploy2Updated),
                          )

          // Inserting data into DB

          // Inserting into data tables
          validator1Id <- blockDbApi.validatorDbApi.insert(validator1Updated)
          validator2Id <- blockDbApi.validatorDbApi.insert(validator2Updated)
          deploy1Id    <- blockDbApi.deployDbApi.insert(deploy1Updated)
          deploy2Id    <- blockDbApi.deployDbApi.insert(deploy2Updated)
          bondId       <- blockDbApi.bondDbApi.insert(Bond(validator1Updated, bond.stake), validator1Id)
          blockId      <- blockDbApi.insert(blockUpdated, validator1Id)

          // Inserting into link tables
          _ <- blockDbApi.blockJustificationsDbApi.insert(BlockJustifications(validator1Id, blockId))
          _ <- blockDbApi.blockJustificationsDbApi.insert(BlockJustifications(validator2Id, blockId))
          _ <- blockDbApi.blockDeploysDbApi.insert(BlockDeploys(blockId, deploy1Id))
          _ <- blockDbApi.blockDeploysDbApi.insert(BlockDeploys(blockId, deploy2Id))
          _ <- blockDbApi.blockBondsDbApi.insert(BlockBonds(blockId, bondId))

          // Getting data from DB
          blockById   <- OptionT(blockDbApi.getById(blockId)).getOrRaise(new RecordNotFound)
          blockByHash <- OptionT(blockDbApi.getByHash(block.hash)).getOrRaise(new RecordNotFound)

        } yield {
          blockId shouldBe 1L
          blockById shouldBe blockUpdated
          blockByHash shouldBe blockUpdated
        }
        LiquiPgSql[IO]
          .map { implicit c =>
            BlockDbApiImpl(
              new ValidatorDbApiImpl[IO],
              new BlockJustificationsDbApiImpl[IO],
              new BlockBondsDbApiImpl[IO],
              new BlockDeploysDbApiImpl[IO],
              new BondDbApiImpl[IO],
              new DeployDbApiImpl[IO],
            )
          }
          .use(test)
          .unsafeRunSync()

    }
  }
}
