package io.rhonix.rholang.normalizer

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import coop.rchain.rholang.interpreter.compiler.{NameSort, VarSort}
import io.rhonix.rholang.normalizer.util.Mock.*
import io.rhonix.rholang.normalizer.util.MockNormalizerRec.mockADT
import io.rhonix.rholang.ast.rholang.Absyn.*
import io.rhonix.rholang.types.{NewN, ParN}
import org.scalacheck.Arbitrary.arbString
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class NewNormalizerSpec extends AnyFlatSpec with ScalaCheckPropertyChecks with Matchers {

  behavior of "New normalizer"

  it should "normalize the new term body, sort and add bound variables in the correct order" in {
    forAll { (bodyStr: String, bindsStr: Seq[String], urnsData: Map[String, String]) =>
      // Map[Uri, Name] is used to ensure that each Uri is unique
      val urnsStr                  = urnsData.toSeq
      val body                     = new PGround(new GroundString(bodyStr))
      val bindsTerm: Seq[NameDecl] = bindsStr.map(new NameDeclSimpl(_))
      val urisStrQuoted            = urnsStr.map { case (uri, name) => (s"`$uri`", name) }
      val urisTerm: Seq[NameDecl]  = urisStrQuoted.map { case (uriQuoted, name) => new NameDeclUrn(name, uriQuoted) }

      val declsWithRandomOrder = scala.util.Random.shuffle(bindsTerm ++ urisTerm)

      val listNameDecl = new ListNameDecl()
      declsWithRandomOrder.foreach(listNameDecl.add)

      val term = new PNew(listNameDecl, body)

      implicit val (nRec, bVScope, bVW, _, _, _, _, _, _) = createMockDSL[IO, VarSort]()

      val adt = NewNormalizer.normalizeNew[IO, VarSort](term).unsafeRunSync()

      val expectedAdt = NewN(
        bindCount = bindsStr.size + urnsData.size,
        p = mockADT(body),
        uri = urnsStr.map(_._1),
        injections = Map[String, ParN](),
      )

      adt shouldBe expectedAdt

      val sortedUris = urnsStr.sortBy(_._1) // Sort by Uri

      // Checking that bound variables are added to the scope in the correct order
      val expectedUnsortedSimpleBinds = declsWithRandomOrder.collect { case n: NameDeclSimpl => n.var_ }
      val expectedSortedUrnNames      = sortedUris.map(_._2)
      val expectedBoundVars           =
        (expectedUnsortedSimpleBinds ++ expectedSortedUrnNames).map(
          BoundVarWriterData(_, varType = NameSort, copyScopeLevel = 1),
        )
      val addedBoundVars              = bVW.extractData
      addedBoundVars shouldBe expectedBoundVars
    }
  }
}
