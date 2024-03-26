package coop.rchain.rholang.interpreter

import cats.Parallel
import cats.effect.unsafe.implicits.global
import cats.effect.{Async, IO}
import cats.implicits.*
import coop.rchain.metrics
import coop.rchain.metrics.*
import coop.rchain.models.Expr.ExprInstance.GString
import coop.rchain.models.rholang.RhoType.RhoBoolean
import coop.rchain.models.rholang.implicits.*
import coop.rchain.rholang.Resources.mkRuntime
import coop.rchain.rholang.interpreter.errors.InterpreterError
import coop.rchain.rholang.syntax.*
import coop.rchain.shared.Log
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import scala.concurrent.duration.*

class VarRefSpec extends AnyFlatSpec with ScalaCheckPropertyChecks with Matchers {
  import VarRefSpec.*

  "Rholang VarRef" should "match in single level depth pattern" in {
    val term =
      s"""
         #  match 42 {
         #    x => {
         #      match 42 {
         #        =x => @"$OutcomeCh"!(true)
         #        _  => @"$OutcomeCh"!(false)
         #      }
         #    }
         #  }
         # """.stripMargin('#')
    testExecute[IO](term).timeout(MaxDuration).unsafeRunSync() should equal(Right(true))
  }

  it should "match in two level depth pattern" in {
    val term =
      s"""
         #  match 42 {
         #    x => {
         #      match {for(@42 <- @Nil) {Nil}} {
         #        {for(@{=x} <- @Nil) { Nil }} => @"$OutcomeCh"!(true)
         #        _                            => @"$OutcomeCh"!(false)
         #      }
         #    }
         #  }
         # """.stripMargin('#')
    testExecute[IO](term).timeout(MaxDuration).unsafeRunSync() should equal(Right(true))
  }
}

object VarRefSpec {
  val MaxDuration: FiniteDuration = 5.seconds
  val OutcomeCh                   = "ret"

  private def testExecute[F[_]: Async: Parallel](
    source: String,
  ): F[Either[InterpreterError, Boolean]] = {
    implicit val logF: Log[F]            = Log.log[F]
    implicit val noopMetrics: Metrics[F] = new metrics.Metrics.MetricsNOP[F]
    implicit val noopSpan: Span[F]       = NoopSpan[F]()

    mkRuntime[F]("rholang-variable-reference")
      .use { runtime =>
        for {
          evalResult <- runtime.evaluate(source)
          result     <- if (evalResult.errors.isEmpty)
                          for {
                            data                       <- runtime.getData(GString(OutcomeCh)).map(_.head)
                            Seq(RhoBoolean(boolResult)) = data.a.pars
                          } yield Right(boolResult)
                        else Left(evalResult.errors.head).pure[F]
        } yield result
      }
  }

}
