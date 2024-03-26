package io.rhonix.rholang.normalizer

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import coop.rchain.rholang.interpreter.compiler.VarSort
import io.rhonix.rholang.normalizer.util.Mock.*
import io.rhonix.rholang.normalizer.util.MockNormalizerRec.{mockADT, RemainderADTDefault}
import io.rhonix.rholang.ast.rholang.Absyn.*
import io.rhonix.rholang.types.{EListN, EMapN, ESetN, ETupleN}
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class CollectNormalizerSpec extends AnyFlatSpec with ScalaCheckPropertyChecks with Matchers {

  "list normalizer" should "normalize CollectList term" in {
    forAll { (strings: Seq[String]) =>
      val gStrings      = strings.map(x => new PGround(new GroundString(x)))
      val listProc      = new ListProc()
      gStrings.foreach(listProc.add)
      val remainderTerm = new ProcRemainderEmpty()

      // [x, y, ...]
      val term = new PCollect(new CollectList(listProc, remainderTerm))

      implicit val (mockRec, _, _, _, _, _, _, _, _) = createMockDSL[IO, VarSort]()

      val adt = CollectNormalizer.normalizeCollect[IO](term).unsafeRunSync()

      // Expect right converted ADT term
      val expectedAdt = EListN(gStrings.map(x => mockADT(x: Proc)), RemainderADTDefault)

      adt shouldBe expectedAdt

      val terms         = mockRec.extractData
      // Starting with the remainder and then proceeding to the next elements
      val expectedTerms = TermData(ProcRemainderTerm(remainderTerm)) +: gStrings.map(x => TermData(ProcTerm(x)))

      terms shouldBe expectedTerms
    }
  }

  "tuple normalizer" should "normalize CollectTuple term" in {
    forAll(Gen.nonEmptyListOf[String](Arbitrary.arbitrary[String])) { strings: Seq[String] =>
      val gStrings = strings.map(x => new PGround(new GroundString(x)))

      val tuple =
        if (gStrings.size == 1) new TupleSingle(gStrings.head)
        else {
          val tailProc = new ListProc()
          gStrings.tail.foreach(tailProc.add)
          new TupleMultiple(gStrings.head, tailProc)
        }

      // (x, y, ...)
      val term = new PCollect(new CollectTuple(tuple))

      implicit val (mockRec, _, _, _, _, _, _, _, _) = createMockDSL[IO, VarSort]()

      // Run Par normalizer
      val adt = CollectNormalizer.normalizeCollect[IO](term).unsafeRunSync()

      // Expect right converted ADT term
      val expectedAdt = ETupleN(gStrings.map(x => mockADT(x: Proc)))

      adt shouldBe expectedAdt

      val terms         = mockRec.extractData
      // Starting with the remainder and then proceeding to the next elements
      val expectedTerms = gStrings.map(x => TermData(ProcTerm(x)))

      terms shouldBe expectedTerms
    }
  }

  "set normalizer" should "normalize CollectSet term" in {
    forAll { (strings: Set[String]) =>
      val gStrings      = strings.toSeq.map(x => new PGround(new GroundString(x)))
      val listProc      = new ListProc()
      gStrings.foreach(listProc.add)
      val remainderTerm = new ProcRemainderEmpty()

      // Set(x, y, ...)
      val term = new PCollect(new CollectSet(listProc, remainderTerm))

      implicit val (mockRec, _, _, _, _, _, _, _, _) = createMockDSL[IO, VarSort]()

      // Run Par normalizer
      val adt = CollectNormalizer.normalizeCollect[IO](term).unsafeRunSync()

      // Expect right converted ADT term
      val expectedAdt = ESetN(gStrings.map(x => mockADT(x: Proc)), RemainderADTDefault)

      adt shouldBe expectedAdt

      val terms         = mockRec.extractData
      // Starting with the remainder and then proceeding to the next elements
      val expectedTerms = TermData(ProcRemainderTerm(remainderTerm)) +: gStrings.map(x => TermData(ProcTerm(x)))

      terms shouldBe expectedTerms
    }
  }

  "map normalizer" should "normalize CollectMap term" in {
    forAll { (stringPairs: Map[String, String]) =>
      val procPairs     = stringPairs.toSeq.map { case (k, v) =>
        (new PGround(new GroundString(k)), new PGround(new GroundString(v)))
      }
      val mapData       = new ListKeyValuePair()
      procPairs.foreach(x => mapData.add(new KeyValuePairImpl(x._1, x._2)))
      val remainderTerm = new ProcRemainderEmpty()

      // {x: y,  k: l, ...}
      val term = new PCollect(new CollectMap(mapData, remainderTerm))

      implicit val (mockRec, _, _, _, _, _, _, _, _) = createMockDSL[IO, VarSort]()

      val adt = CollectNormalizer.normalizeCollect[IO](term).unsafeRunSync()

      // Expect right converted ADT term
      val expectedAdt = EMapN(procPairs.map(x => (mockADT(x._1: Proc), mockADT(x._2: Proc))), RemainderADTDefault)

      adt shouldBe expectedAdt

      val terms         = mockRec.extractData
      // Starting with the remainder and then proceeding to the next elements
      val expectedTerms =
        TermData(ProcRemainderTerm(remainderTerm)) +: procPairs
          .flatten(x => Seq(x._1, x._2))
          .map(x => TermData(ProcTerm(x)))

      terms shouldBe expectedTerms
    }
  }
}
