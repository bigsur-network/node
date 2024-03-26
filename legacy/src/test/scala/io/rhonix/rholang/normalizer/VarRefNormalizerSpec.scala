package io.rhonix.rholang.normalizer

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import coop.rchain.rholang.interpreter.compiler.{NameSort, ProcSort, VarSort}
import coop.rchain.rholang.interpreter.errors.{InterpreterError, UnboundVariableRef}
import io.rhonix.rholang.normalizer.util.Mock.*
import io.rhonix.rholang.ast.rholang.Absyn.*
import io.rhonix.rholang.types.ConnVarRefN
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class VarRefNormalizerSpec extends AnyFlatSpec with ScalaCheckPropertyChecks with Matchers {

  behavior of "VarRef normalizer"

  implicit val arbStringVarSort: Arbitrary[(String, VarSort)] = Arbitrary {
    for {
      str     <- Arbitrary.arbitrary[String]
      varSort <- Gen.oneOf(ProcSort, NameSort)
    } yield (str, varSort)
  }

  it should "find a given var among other bound variables and construct ADT term" in {
    forAll { (searchVarData: (String, VarSort), anotherVars: Map[String, VarSort]) =>
      val (varName, varType) = searchVarData
      val InitBoundVarsData  = (anotherVars + searchVarData).zipWithIndex.map { case ((name, sort), index) =>
        name -> (index, sort)
      }.toMap
      val varRefKind         = varType match {
        case ProcSort => new VarRefKindProc()
        case NameSort => new VarRefKindName()
      }

      val term = new PVarRef(varRefKind, varName)

      implicit val (_, _, _, bWR, _, _, _, _, nReader) = createMockDSL[IO, VarSort](initBoundVars = InitBoundVarsData)

      val adt = VarRefNormalizer.normalizeVarRef[IO, VarSort](term).unsafeRunSync()

      val expectedAdt = ConnVarRefN(index = InitBoundVarsData(varName)._1, depth = 0)

      adt shouldBe expectedAdt
    }
  }

  it should "throw an exception if a given var is not found among bound variables" in {
    forAll { (searchVarData: (String, VarSort)) =>
      val (varName, varType) = searchVarData
      val varRefKind         = varType match {
        case ProcSort => new VarRefKindProc()
        case NameSort => new VarRefKindName()
      }
      val term               = new PVarRef(varRefKind, varName)

      implicit val (_, _, _, bWR, _, _, _, _, nReader) = createMockDSL[IO, VarSort](
        initBoundVars = Map.empty,
      )

      val thrown = intercept[UnboundVariableRef] {
        VarRefNormalizer.normalizeVarRef[IO, VarSort](term).unsafeRunSync()
      }

      thrown.getMessage should include(varName)
    }
  }

  it should "throw an exception if type of a given var is not expected" in {
    forAll { (searchVarData: (String, VarSort)) =>
      val (varName, varType) = searchVarData
      val varRefKind         = varType match {
        case ProcSort => new VarRefKindProc()
        case NameSort => new VarRefKindName()
      }
      val term               = new PVarRef(varRefKind, varName)

      val unexpectedVarType = varType match { case ProcSort => NameSort; case NameSort => ProcSort }

      implicit val (_, _, _, bWR, _, _, _, _, nReader) = createMockDSL[IO, VarSort](
        initBoundVars = Map(varName -> (0, unexpectedVarType)),
      )

      val thrown = intercept[InterpreterError] {
        VarRefNormalizer.normalizeVarRef[IO, VarSort](term).unsafeRunSync()
      }
      thrown.getMessage should include(varName)
    }
  }
}
