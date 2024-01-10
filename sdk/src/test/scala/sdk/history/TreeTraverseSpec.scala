package sdk.history

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import cats.implicits.{catsSyntaxApplicativeId, catsSyntaxApply, catsSyntaxEitherId}
import org.scalacheck.Gen.tailRecM
import org.scalacheck.ScalacheckShapeless.derivedArbitrary
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{EitherValues, OptionValues}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll
import sdk.hashing.Sha256.*
import sdk.history.TreeTraverseSpec.*
import sdk.history.RadixTree.*
import sdk.history.instances.RadixHistory
import sdk.primitive.ByteArray
import sdk.store.{InMemoryKeyValueStore, KeyValueTypedStore}
import sdk.syntax.all.*

import scala.collection.immutable.Seq
import scala.concurrent.duration.*

class TreeTraverseSpec extends AnyFlatSpec with Matchers with OptionValues with EitherValues {
  "parTreeTraverse" should "export all data for random tree" in {
    forAll(generateRandomTree(10000, 1, 256)) { tree =>
      val exportedStream = parTreeTraverse[IO, KeyType, ValueType](Seq(tree.root), tree.data.getUnsafe(_).pure[IO])
      val collectedData  = exportedStream.compile.toList.unsafeRunSync().toMap
      val storedData     = tree.data.map(n => (n._1, n._2._1))
      storedData shouldBe collectedData
    }
  }

  "parTreeTraverse" should "be stack safe" in {
    val maxLevel = findMaxRecursionLevel() // TODO: Runs for a very long time when processed in parallel
    forAll(generateRandomTree(200, 1, 1)) { tree =>
      val exportedStream = parTreeTraverse[IO, KeyType, ValueType](Seq(tree.root), tree.data.getUnsafe(_).pure[IO])
      val collectedData  = exportedStream.compile.toList.unsafeRunSync().toMap
      val storedData     = tree.data.map(n => (n._1, n._2._1))
      storedData shouldBe collectedData
    }
  }
}

object TreeTraverseSpec {
  type KeyType   = Int
  type ValueType = Int
  case class Tree(root: KeyType, data: Map[KeyType, (ValueType, Seq[KeyType])])

  def generateRandomTree(size: Int, childNumMin: Int, childNumMax: Int): Gen[Tree] = {
    case class KVPair(k: KeyType, v: ValueType)
    case class Node(k: KeyType, v: ValueType, children: Seq[KeyType])

    case class PmsLoopNodeIn(
      forProcessing: Seq[KVPair],
      children: Seq[KVPair],
      nextAcc: Seq[KVPair],
      nodeAcc: Seq[Node],
    )
    case class PmsLoopNodeOut(next: Seq[KVPair], children: Seq[KVPair], processed: Seq[Node])
    def loopNode(in: PmsLoopNodeIn): Gen[Either[PmsLoopNodeIn, PmsLoopNodeOut]] =
      Gen.chooseNum(childNumMin, childNumMax).map { size =>
        if (in.forProcessing.isEmpty)
          PmsLoopNodeOut(next = in.nextAcc, children = in.children, processed = in.nodeAcc).asRight
        else {
          val curKV         = in.forProcessing.head
          val curSize       = if (in.children.size >= size) size else in.children.size
          val curChildren   = in.children.take(curSize)
          val newNode: Node = Node(curKV.k, curKV.v, curChildren.map(_.k))
          PmsLoopNodeIn(
            forProcessing = in.forProcessing.tail,
            children = in.children.drop(curSize),
            nextAcc = in.nextAcc ++ curChildren,
            nodeAcc = in.nodeAcc :+ newNode,
          ).asLeft
        }
      }

    case class PmsLoopBreadthIn(kVs: Seq[KVPair], children: Seq[KVPair], acc: Seq[Node])
    def loopBreadth(in: PmsLoopBreadthIn): Gen[Either[PmsLoopBreadthIn, Seq[Node]]] =
      tailRecM(PmsLoopNodeIn(in.kVs, in.children, Seq.empty, Seq.empty))(loopNode).map {
        case PmsLoopNodeOut(next, children, processed) =>
          if (next.isEmpty) (in.acc ++ processed).asRight
          else PmsLoopBreadthIn(kVs = next, children = children, acc = in.acc ++ processed).asLeft

      }

    require(size > 0, "Size of tree is zero")

    for {
      randomMap <- Gen.mapOfN(size, Arbitrary.arbitrary[(KeyType, ValueType)])
      kVs        = randomMap.toSeq.map(kv => KVPair(kv._1, kv._2))
      root       = kVs.head
      res       <- tailRecM(PmsLoopBreadthIn(Seq(root), kVs.tail, Seq.empty))(loopBreadth)
    } yield Tree(root.k, res.map(n => (n.k, (n.v, n.children))).toMap)
  }

  def findMaxRecursionLevel(): Int = {
    def loop(level: Int): Int = try loop(level + 1)
    catch {
      case _: StackOverflowError => level
    }
    loop(0)
  }

}
