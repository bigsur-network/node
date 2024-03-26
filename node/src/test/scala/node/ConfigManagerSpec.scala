package node

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import node.ConfigManager.DefaultConfig
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import slick.SlickSpec.embedPgSlick

class ConfigManagerSpec extends AnyFlatSpec with Matchers {
  "Configs saved to DB and loaded from DB" should "be the same" in {
    val saved = DefaultConfig
    embedPgSlick[IO]
      .use { db =>
        ConfigManager.writeAll[IO](saved._1, saved._2, saved._3, db).flatMap { _ =>
          ConfigManager.loadAll[IO](db).map { loaded =>
            saved shouldBe loaded
          }
        }
      }
      .unsafeRunSync()
  }
}
