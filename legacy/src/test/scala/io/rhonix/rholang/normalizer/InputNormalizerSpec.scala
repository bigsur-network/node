package io.rhonix.rholang.normalizer

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import coop.rchain.rholang.interpreter.compiler.{NameSort, VarSort}
import coop.rchain.rholang.interpreter.errors.ReceiveOnSameChannelsError
import io.rhonix.rholang.normalizer.util.Mock.*
import io.rhonix.rholang.normalizer.util.MockNormalizerRec.{mockADT, RemainderADTDefault}
import io.rhonix.rholang.ast.rholang.Absyn.*
import io.rhonix.rholang.types.{ReceiveBindN, ReceiveN}
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class InputNormalizerSpec extends AnyFlatSpec with ScalaCheckPropertyChecks with Matchers {

  behavior of "Input normalizer"

  it should "normalize a simple receive" in {
    forAll(Arbitrary.arbitrary[Map[String, Seq[String]]]) { (bindsData: Map[String, Seq[String]]) =>
      val continuation = new PNil
      val remainder    = new NameRemainderEmpty()

      // We used a Map because each sourceStr should be unique
      val varTerms = bindsData.toSeq.map { case (sourceStr, bindsStr) =>
        (new NameVar(sourceStr), bindsStr.map(new NameVar(_)))
      }

      val linearBinds = varTerms.map { case (sourceVar, bindsVar) =>
        val listBindings = new ListName()
        bindsVar.foreach(listBindings.add)

        new LinearBindImpl(
          listBindings,
          new NameRemainderEmpty(),
          new SimpleSource(sourceVar),
        )
      }

      val listLinearBinds = new ListLinearBind()
      linearBinds.foreach(listLinearBinds.add)
      val linearSimple    = new LinearSimple(listLinearBinds)
      val receipt         = new ReceiptLinear(linearSimple)
      val listReceipt     = new ListReceipt()
      listReceipt.add(receipt)

      // for (binds1 <- source1 & binds2 <- source2 & ...) { continuation }
      val term = new PInput(listReceipt, continuation)

      implicit val (nRec, bVScope, bVW, _, fVScope, _, fVR, infoWriter, _) = createMockDSL[IO, VarSort]()

      val adt = InputNormalizer.normalizeInput[IO, VarSort](term).unsafeRunSync()

      val expectedAdt = ReceiveN(
        binds = varTerms.map { case (sourceVar, bindsVar) =>
          ReceiveBindN(
            patterns = bindsVar.map(mockADT),
            source = mockADT(sourceVar),
            remainder = RemainderADTDefault,
            freeCount = 0, // We don't care about free variables in this test
          )
        },
        body = mockADT(continuation),
        persistent = false,
        peek = false,
        bindCount = 0, // We don't care about free variables in this test
      )

      adt shouldBe expectedAdt

      val terms                     = nRec.extractData
      // Expect all terms to be normalized in sequence
      val (sourcesVar, bindsSeqVar) = varTerms.unzip

      val expectedTerms = sourcesVar.map(x => TermData(NameTerm(x))) ++ bindsSeqVar.flatMap(
        _.map(bind =>
          TermData(
            NameTerm(bind),
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
          ),
      ) :+ TermData(ProcTerm(continuation), boundCopyScopeLevel = 1)

      terms shouldBe expectedTerms
    }
  }

  it should "bind free variables before case body normalization" in {
    forAll { (varsNameStr: Seq[String]) =>

      val listReceipt = new ListReceipt()
      listReceipt.add(new ReceiptLinear(new LinearSimple(new ListLinearBind())))
      val term        = new PInput(listReceipt, new PNil)

      val initFreeVars = varsNameStr.distinct.zipWithIndex.map { case (name, index) => (name, (index, NameSort)) }.toMap

      implicit val (nRec, bVScope, bVW, _, fVScope, _, fVR, infoWriter, _) =
        createMockDSL[IO, VarSort](initFreeVars = initFreeVars)

      InputNormalizer.normalizeInput[IO, VarSort](term).unsafeRunSync()

      val addedBoundVars = bVW.extractData

      val expectedBoundVars = initFreeVars.keys.toSeq.map(BoundVarWriterData(_, varType = NameSort, copyScopeLevel = 1))

      addedBoundVars.toSet shouldBe expectedBoundVars.toSet
    }
  }

  it should "normalize a simple persistent receive" in {
    val listReceipt  = new ListReceipt()
    listReceipt.add(new ReceiptRepeated(new RepeatedSimple(new ListRepeatedBind())))
    val continuation = new PNil
    // for (pattern <= source) { Nil }
    val term         = new PInput(listReceipt, continuation)

    implicit val (nRec, bVScope, bVW, _, fVScope, _, fVR, infoWriter, _) = createMockDSL[IO, VarSort]()

    val adt = InputNormalizer.normalizeInput[IO, VarSort](term).unsafeRunSync()

    val expectedAdt = ReceiveN(
      binds = Seq(),
      body = mockADT(continuation),
      persistent = true,
      peek = false,
      bindCount = 0,
    )

    adt shouldBe expectedAdt
  }

  it should "normalize a simple peek receive" in {
    val listReceipt  = new ListReceipt()
    listReceipt.add(new ReceiptPeek(new PeekSimple(new ListPeekBind())))
    val continuation = new PNil
    // for (pattern <<- source) { Nil }
    val term         = new PInput(listReceipt, continuation)

    implicit val (nRec, bVScope, bVW, _, fVScope, _, fVR, infoWriter, _) = createMockDSL[IO, VarSort]()

    val adt = InputNormalizer.normalizeInput[IO, VarSort](term).unsafeRunSync()

    val expectedAdt = ReceiveN(
      binds = Seq(),
      body = mockADT(continuation),
      persistent = false,
      peek = true,
      bindCount = 0,
    )

    adt shouldBe expectedAdt
  }

  it should "normalize a complex receive" in {
    val listReceipt  = new ListReceipt()
    listReceipt.add(new ReceiptLinear(new LinearSimple(new ListLinearBind())))
    val continuation = new PNil
    val term         = new PInput(listReceipt, continuation)

    implicit val (nRec, bVScope, bVW, _, fVScope, _, fVR, infoWriter, _) = createMockDSL[IO, VarSort]()

    val adt = InputNormalizer.normalizeInput[IO, VarSort](term).unsafeRunSync()

    val expectedAdt = ReceiveN(
      binds = Seq(),
      body = mockADT(continuation),
      persistent = false,
      peek = false,
      bindCount = 0,
    )

    adt shouldBe expectedAdt
  }

  it should "throw an exception if binds contain not unique sources" in {
    val source1   = new SimpleSource(new NameVar("source1"))
    val source2   = new SimpleSource(new NameVar("source2"))
    val listName1 = new ListName()
    listName1.add(new NameVar("x"))
    val listName2 = new ListName()
    listName2.add(new NameVar("y"))
    val listName3 = new ListName()
    listName3.add(new NameVar("z"))

    val linearBinds = Seq(
      new LinearBindImpl(listName1, new NameRemainderEmpty(), source1),
      new LinearBindImpl(listName2, new NameRemainderEmpty(), source2),
      new LinearBindImpl(listName3, new NameRemainderEmpty(), source1),
    )

    val listLinearBind = new ListLinearBind()
    linearBinds.foreach(listLinearBind.add)

    val linearSimple = new LinearSimple(listLinearBind)
    val receipt      = new ReceiptLinear(linearSimple)
    val listReceipt  = new ListReceipt()
    listReceipt.add(receipt)

    val term = new PInput(listReceipt, new PNil)

    implicit val (nRec, bVScope, bVW, _, fVScope, _, fVR, infoWriter, _) = createMockDSL[IO, VarSort]()

    val result = InputNormalizer.normalizeInput[IO, VarSort](term)

    val thrown = intercept[Throwable] {
      result.unsafeRunSync()
    }

    thrown shouldBe a[ReceiveOnSameChannelsError]
  }

  it should "normalize a receive with several receipts" in {
    forAll(
      Arbitrary.arbitrary[(String, String)],
      Gen.nonEmptyListOf[(String, String)](Arbitrary.arbitrary[(String, String)]),
    ) { (firstReceiptData: (String, String), anotherReceiptData: Seq[(String, String)]) =>
      val continuation: Proc = new PNil
      val remainder          = new NameRemainderEmpty()

      val bindsData = firstReceiptData +: anotherReceiptData

      val varTerms = bindsData.map { case (sourceStr, bindStr) =>
        (new NameVar(sourceStr), new NameVar(bindStr))
      }

      val receipts = varTerms.map { case (sourceVar, bindsVar) =>
        val listBindings = new ListName()
        listBindings.add(bindsVar)

        val linearBind = new LinearBindImpl(
          listBindings,
          remainder,
          new SimpleSource(sourceVar),
        )

        val listLinearBind = new ListLinearBind()
        listLinearBind.add(linearBind)

        val linearSimple = new LinearSimple(listLinearBind)
        new ReceiptLinear(linearSimple)
      }

      val listReceipt = new ListReceipt()
      receipts.foreach(listReceipt.add)

      // for (receipt1; receipt2) { continuation }
      val term = new PInput(listReceipt, continuation)

      implicit val (nRec, bVScope, bVW, _, fVScope, _, fVR, infoWriter, _) = createMockDSL[IO, VarSort]()

      val adt = InputNormalizer.normalizeInput[IO, VarSort](term).unsafeRunSync()

      val expectedListReceipt = new ListReceipt()
      expectedListReceipt.add(receipts.head)

      /* Processing of joins, for example: for(x <- @"ch1"; y <- @"ch2") {Nil}
      will be normalized as nested inputs: for(x <- @"ch1") { for(y <- @"ch2") {Nil} } */
      val expectedInputContinuation = receipts.tail.reverse.foldLeft(continuation) { case (proc, receipt) =>
        val receiptList = new ListReceipt()
        receiptList.add(receipt)
        new PInput(receiptList, proc)
      }

      val reconstructedTerm = new PInput(expectedListReceipt, expectedInputContinuation)

      val expectedAdt = mockADT(reconstructedTerm)
      adt shouldBe expectedAdt

      val terms         = nRec.extractData
      val expectedTerms = Seq(TermData(ProcTerm(reconstructedTerm)))

      terms shouldBe expectedTerms
    }
  }

  /* Normalizing input with complex sources (ReceiveSendSource and SendReceiveSource)
  is not checked in this test. This is because it requires knowledge of the unique identifier
  generated during the construction of a new par. This aspect should be covered in another higher-level unit test.
   */

}
