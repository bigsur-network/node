package io.rhonix.rholang.normalizer

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import coop.rchain.rholang.interpreter.compiler.{NameSort, ProcSort, VarSort}
import io.rhonix.rholang.normalizer.util.Mock.*
import io.rhonix.rholang.normalizer.util.MockNormalizerRec.{mockADT, RemainderADTDefault}
import io.rhonix.rholang.ast.rholang.Absyn.*
import io.rhonix.rholang.types.{ReceiveBindN, ReceiveN}
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class ContrNormalizerSpec extends AnyFlatSpec with ScalaCheckPropertyChecks with Matchers {

  behavior of "Contr normalizer"

  it should "normalize PContr term" in {
    forAll(Arbitrary.arbitrary[String], Gen.nonEmptyListOf[String](Arbitrary.arbitrary[String])) {
      (sourceStr: String, strings: Seq[String]) =>
        val source       = new NameVar(sourceStr)
        val patterns     = strings.distinct.map(new NameVar(_))
        val remainder    = new NameRemainderEmpty()
        val continuation = new PNil

        val listPatterns = new ListName()
        patterns.foreach(listPatterns.add)

        // contract source (pattern1, pattern2, ... remainder) { continuation }
        val term = new PContr(source, listPatterns, remainder, continuation)

        implicit val (nRec, bVScope, bVW, _, fVScope, _, fVR, infoWriter, _) = createMockDSL[IO, VarSort]()

        val adt = ContractNormalizer.normalizeContract[IO, VarSort](term).unsafeRunSync()

        val expectedAdt = ReceiveN(
          bind = ReceiveBindN(
            patterns = patterns.map(mockADT),
            source = mockADT(source),
            remainder = RemainderADTDefault,
            freeCount = 0, // We don't care about free variables in this test
          ),
          body = mockADT(continuation),
          persistent = true,
          peek = false,
          bindCount = 0, // We don't care about free variables in this test
        )

        adt shouldBe expectedAdt

        val terms         = nRec.extractData
        // Expect all terms to be normalized in sequence
        val expectedTerms = TermData(NameTerm(source)) +:
          patterns.map(x =>
            TermData(
              NameTerm(x),
              boundNewScopeLevel = 1,
              freeScopeLevel = 1,
              insidePattern = true,
              insideTopLevelReceive = true,
            ),
          ) :+
          TermData(
            NameRemainderTerm(remainder),
            boundNewScopeLevel = 1,
            freeScopeLevel = 1,
            insidePattern = true,
            insideTopLevelReceive = true,
          ) :+
          TermData(ProcTerm(continuation), boundCopyScopeLevel = 1)
        terms shouldBe expectedTerms
    }
  }

  it should "bind free variables before case body normalization" in {
    forAll { (varsNameStr: Seq[String]) =>

      val listPatterns = new ListName()
      listPatterns.add(new NameVar(""))
      val term         = new PContr(new NameVar(""), listPatterns, new NameRemainderEmpty(), new PNil)

      val initFreeVars = varsNameStr.distinct.zipWithIndex.map { case (name, index) => (name, (index, NameSort)) }.toMap

      implicit val (nRec, bVScope, bVW, _, fVScope, _, fVR, infoWriter, _) =
        createMockDSL[IO, VarSort](initFreeVars = initFreeVars)

      ContractNormalizer.normalizeContract[IO, VarSort](term).unsafeRunSync()

      val addedBoundVars = bVW.extractData

      // Absorbed free variables and bind them in a copy of the scope
      val expectedBoundVars = initFreeVars.keys.toSeq.map(BoundVarWriterData(_, varType = NameSort, copyScopeLevel = 1))

      addedBoundVars.toSet shouldBe expectedBoundVars.toSet
    }
  }
}
