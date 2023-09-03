package db

import cats.data.OptionT
import cats.effect.unsafe.implicits.global
import cats.effect.{IO, Sync}
import cats.syntax.all.*
import io.zonky.test.db.postgres.embedded.LiquibasePreparer
import io.zonky.test.db.postgres.junit5.{EmbeddedPostgresExtension, PreparedDbExtension}
import org.junit.jupiter.api.extension.ExtensionContext
import org.mockito.Mockito.mock
import org.scalacheck.ScalacheckShapeless.*
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import sdk.api.data.*
import sdk.db.*
import squeryl.*

// TODO re-enable these tests when it is possible to execute them on CI.
// At the moment Github Actions does not allow non-root users and embedded postgres requires non-root.
// https://github.com/fergusstrange/embedded-postgres/issues/95
class DbSpec extends AsyncFlatSpec with Matchers with ScalaCheckPropertyChecks {

  "Validator insert function call" should "add the correct entry to the Validator table" ignore {
    forAll { (validator: Validator) =>
      (for {
        session           <- makeSession[IO]
        validatorDbApi    <- makeDbApi(session)(implicit s => new ValidatorDbApiImpl[IO])
        id                <- validatorDbApi.insert(validator)
        validatorById     <- OptionT(validatorDbApi.getById(id)).getOrRaise(new RecordNotFound)
        validatorByPubKey <- OptionT(validatorDbApi.getByPublicKey(validator.publicKey)).getOrRaise(new RecordNotFound)
      } yield {
        id shouldBe 1L

        validatorById.publicKey shouldBe validator.publicKey
        validatorById.http shouldBe validator.http

        validatorById shouldBe validatorByPubKey
      }).unsafeRunSync()
    }
  }

  "Bond insert function call" should "add the correct entry to the Bond table" ignore {
    forAll { (validator: Validator, bondStake: Long) =>
      (for {
        session        <- makeSession[IO]
        validatorDbApi <- makeDbApi(session)(implicit s => new ValidatorDbApiImpl[IO])
        validatorId    <- validatorDbApi.insert(validator)
        _              <- OptionT[IO, Validator](validatorDbApi.getById(validatorId)).getOrRaise(new RecordNotFound)
        bondDbApi      <- makeDbApi(session)(implicit s => new BondDbApiImpl[IO])
        bondId         <- bondDbApi.insert(Bond(validator, bondStake), validatorId)
        bond           <- OptionT(bondDbApi.getById(bondId)).getOrRaise(new RecordNotFound)
      } yield {
        bondId shouldBe 1L
        bond.stake shouldBe bondStake
      }).unsafeRunSync()
    }
  }

  "Deploy insert function call" should "add the correct entry to the Deploy table" ignore {
    forAll { (deploy: Deploy) =>
      (for {
        session      <- makeSession[IO]
        deployDbApi  <- makeDbApi(session)(implicit s => new DeployDbApiImpl[IO])
        deployId     <- deployDbApi.insert(deploy)
        deployById   <- OptionT(deployDbApi.getById(deployId)).getOrRaise(new RecordNotFound)
        deployByHash <- OptionT(deployDbApi.getByHash(deploy.hash)).getOrRaise(new RecordNotFound)
      } yield {
        deployId shouldBe 1L
        deploy shouldBe deployById
        deployById shouldBe deployByHash
      }).unsafeRunSync()
    }
  }

  "Block insert function call" should "add the correct entry to the Block table" ignore {
    forAll { (validator: Validator, block: Block) =>
      (for {
        session <- makeSession[IO]

        validatorDbApi           <- makeDbApi(session)(implicit s => new ValidatorDbApiImpl[IO])
        blockJustificationsDbApi <- makeDbApi(session)(implicit s => new BlockJustificationsDbApiImpl[IO])
        blockBondsDbApi          <- makeDbApi(session)(implicit s => new BlockBondsDbApiImpl[IO])
        blockDeploysDbApi        <- makeDbApi(session)(implicit s => new BlockDeploysDbApiImpl[IO])
        bondDbApi                <- makeDbApi(session)(implicit s => new BondDbApiImpl[IO])
        deployDbApi              <- makeDbApi(session)(implicit s => new DeployDbApiImpl[IO])

        blockDbApi  <- {
          implicit val (v, bj, bb, bd, b, d) =
            (validatorDbApi, blockJustificationsDbApi, blockBondsDbApi, blockDeploysDbApi, bondDbApi, deployDbApi)
          makeDbApi(session)(implicit s => new BlockDbApiImpl[IO])
        }

        // Correction of the generated data
        blockUpdated = block.copy(
                         sender = validator,
                         bonds = Set.empty[Bond],
                         deploys = Set.empty[Deploy],
                         justifications = Set.empty[Validator],
                       )

        // Inserting data into DB
        validatorId <- validatorDbApi.insert(validator)
        blockId     <- blockDbApi.insert(blockUpdated, validatorId)

        // Getting data from DB
        blockById   <- OptionT(blockDbApi.getById(blockId)).getOrRaise(new RecordNotFound)
        blockByHash <- OptionT(blockDbApi.getByHash(block.hash)).getOrRaise(new RecordNotFound)
      } yield {
        blockId shouldBe 1L
        blockById shouldBe blockUpdated
        blockByHash shouldBe blockUpdated
      }).unsafeRunSync()
    }
  }

  "Complex Db logic" should "works correct" ignore {
    forAll {
      (validator1: Validator, validator2: Validator, bond: Bond, deploy1: Deploy, deploy2: Deploy, block: Block) =>
        (for {
          session <- makeSession[IO]

          validatorDbApi           <- makeDbApi(session)(implicit s => new ValidatorDbApiImpl[IO])
          blockJustificationsDbApi <- makeDbApi(session)(implicit s => new BlockJustificationsDbApiImpl[IO])
          blockBondsDbApi          <- makeDbApi(session)(implicit s => new BlockBondsDbApiImpl[IO])
          blockDeploysDbApi        <- makeDbApi(session)(implicit s => new BlockDeploysDbApiImpl[IO])
          bondDbApi                <- makeDbApi(session)(implicit s => new BondDbApiImpl[IO])
          deployDbApi              <- makeDbApi(session)(implicit s => new DeployDbApiImpl[IO])

          blockDbApi       <- {
            implicit val (v, bj, bb, bd, b, d) =
              (validatorDbApi, blockJustificationsDbApi, blockBondsDbApi, blockDeploysDbApi, bondDbApi, deployDbApi)
            makeDbApi(session)(implicit s => new BlockDbApiImpl[IO])
          }

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
          validator1Id <- validatorDbApi.insert(validator1Updated)
          validator2Id <- validatorDbApi.insert(validator2Updated)
          deploy1Id    <- deployDbApi.insert(deploy1Updated)
          deploy2Id    <- deployDbApi.insert(deploy2Updated)
          bondId       <- bondDbApi.insert(Bond(validator1Updated, bond.stake), validator1Id)
          blockId      <- blockDbApi.insert(blockUpdated, validator1Id)

          // Inserting into link tables
          _ <- blockJustificationsDbApi.insert(BlockJustifications(validator1Id, blockId))
          _ <- blockJustificationsDbApi.insert(BlockJustifications(validator2Id, blockId))
          _ <- blockDeploysDbApi.insert(BlockDeploys(blockId, deploy1Id))
          _ <- blockDeploysDbApi.insert(BlockDeploys(blockId, deploy2Id))
          _ <- blockBondsDbApi.insert(BlockBonds(blockId, bondId))

          // Getting data from DB
          blockById   <- OptionT(blockDbApi.getById(blockId)).getOrRaise(new RecordNotFound)
          blockByHash <- OptionT(blockDbApi.getByHash(block.hash)).getOrRaise(new RecordNotFound)

        } yield {
          blockId shouldBe 1L
          blockById shouldBe blockUpdated
          blockByHash shouldBe blockUpdated
        }).unsafeRunSync()
    }
  }

  private def makeSession[F[_]: Sync]: F[DbSession[F]] =
    for {
      database <- Sync[F].delay(makeDb)
      _         = database.beforeEach(mock(classOf[ExtensionContext]))
      session   = squeryl.session[F](database.getTestDatabase.getConnection)
    } yield session

  private def makeDb: PreparedDbExtension =
    EmbeddedPostgresExtension.preparedDatabase(LiquibasePreparer.forClasspathLocation("liquibase/changelog.yaml"))

  private def makeDbApi[F[_]: Sync, T](session: DbSession[F])(creatorFun: DbSession[F] => T): F[T] =
    Sync[F].delay(creatorFun(session))
}
