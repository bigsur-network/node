package sdk.history

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.scalacheck.ScalacheckShapeless.derivedArbitrary
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{EitherValues, OptionValues}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll
import sdk.hashing.Sha256.*
import sdk.history.ExportStateSpec.*
import sdk.history.RadixTree.*
import sdk.history.instances.RadixHistory
import sdk.primitive.ByteArray
import sdk.store.{InMemoryKeyValueStore, KeyValueTypedStore}
import sdk.syntax.all.*

import scala.concurrent.duration.*

class ExportStateSpec extends AnyFlatSpec with Matchers with OptionValues with EitherValues {
  "parTreeTraverse" should "export all tuple-space data for random history" in {
    forAll(generateHistoryData(10000)) { (inputData: Map[KeySegment, ByteArray32]) =>
      withRadixTree { (impl, store, getNext) =>
        val insertActions = inputData.toList.map(kV => InsertAction(kV._1, kV._2))
      for {
        rootNodeAndHashOpt <- impl.saveAndCommit(RadixTree.EmptyNode, insertActions)
        (_, rootHash)       = rootNodeAndHashOpt.get

        exportedStream =
          parTreeTraverse[IO, ByteArray32, ByteArray](Seq(rootHash), getNext)
        collectedData <- exportedStream.compile.toList

        storeData <- store.toMap
        _          = storeData shouldBe collectedData.toMap
      } yield ()
      }
    }
  }

  // This test is necessary to check the stack safety
  "parTreeTraverse" should "export data for history with longest branch" in {
    val printFlag: Boolean = false // Set this flag to print tree
    withRadixTree { (impl, store, getNext) =>
      val insertActions = longestBranch.toList.map(kV => InsertAction(kV._1, kV._2))
      for {
        rootNodeAndHashOpt  <- impl.saveAndCommit(RadixTree.EmptyNode, insertActions)
        (rootNode, rootHash) = rootNodeAndHashOpt.get

        exportedStream =
          parTreeTraverse[IO, ByteArray32, ByteArray](Seq(rootHash), getNext)

        storeData     <- store.toMap
        collectedData <- exportedStream.compile.toList

        _ <- if (printFlag) impl.printTree(rootNode, "Longest branch", noPrintFlag = false) else IO.pure(())
        _  = storeData shouldBe collectedData.toMap
      } yield ()
    }
  }

  // Since trees can intersect, for speedup it is important that the algorithm does not duplicate data during multi-threaded export
  "parTreeTraverse" should "not process history records which already exported" in {
    forAll(generateHistoryData(5000), generateHistoryData(1000), generateHistoryData(1000)) {
      // Build two trees based on the same one. Checking that parallel export does not duplicate data.
      (
        baseData: Map[KeySegment, ByteArray32],
        tree1Data: Map[KeySegment, ByteArray32],
        tree2Data: Map[KeySegment, ByteArray32],
      ) =>
        withRadixTree { (impl, _, getNext) =>
          val baseActions  = baseData.toList.map(kV => InsertAction(kV._1, kV._2))
          val tree1Actions = tree1Data.toList.map(kV => InsertAction(kV._1, kV._2))
          val tree2Actions = tree2Data.toList.map(kV => InsertAction(kV._1, kV._2))
          for {
            baseOpt             <- impl.saveAndCommit(RadixTree.EmptyNode, baseActions)
            (baseNode, baseHash) = baseOpt.get

            tree1Opt      <- impl.saveAndCommit(baseNode, tree1Actions)
            (_, tree1Hash) = tree1Opt.get

            tree2Opt      <- impl.saveAndCommit(baseNode, tree2Actions)
            (_, tree2Hash) = tree2Opt.get

            // Parallel export of trees
            exportedStream     =
              parTreeTraverse[IO, ByteArray32, ByteArray](Seq(baseHash, tree1Hash, tree2Hash), getNext)
            collectedDataList <- exportedStream.compile.toList
            collectedDataMap   = collectedDataList.toMap

            // Checking that there are no duplicates in the exported data
            _ = collectedDataList.size shouldBe collectedDataMap.size
          } yield ()
        }
    }
  }

  // The storage can contain many trees, it is important that exporting one tree does not pull in unnecessary data related to other trees.
  "parTreeTraverse" should "correct export small part of big history" in {
    forAll(generateHistoryData(100), generateHistoryData(10000)) {
      (
        smallTreeData: Map[KeySegment, ByteArray32],
        bigTreeData: Map[KeySegment, ByteArray32],
      ) =>
        withRadixTree { (impl, store, getNext) =>
          val smallTreeActions = smallTreeData.toList.map(kV => InsertAction(kV._1, kV._2))
          val bigTreeActions   = bigTreeData.toList.map(kV => InsertAction(kV._1, kV._2))
          for {
            smallTreeOpt                  <- impl.saveAndCommit(RadixTree.EmptyNode, smallTreeActions)
            (smallTreeNode, smallTreeHash) = smallTreeOpt.get
            // Save copy of data from storage with only a small tree
            smallTreeData                 <- store.toMap

            // Add another tree data in storage on top of a small tree
            _ <- impl.saveAndCommit(smallTreeNode, bigTreeActions)

            // Export only small tree
            exportedStream =
              parTreeTraverse[IO, ByteArray32, ByteArray](Seq(smallTreeHash), getNext)
            collectedData <- exportedStream.compile.toList

            _ = smallTreeData shouldBe collectedData.toMap
          } yield ()
        }
    }
  }

}

object ExportStateSpec {
  def generateHistoryData(size: Int): Gen[Map[KeySegment, ByteArray32]] = {
    implicit val keySegmentGen: Arbitrary[KeySegment]   =
      Arbitrary(Gen.listOfN(32, Gen.alphaChar).map(chars => KeySegment(chars.mkString.getBytes)))
    implicit val byteArray32Gen: Arbitrary[ByteArray32] =
      Arbitrary(Gen.listOfN(32, Gen.alphaChar).map(chars => ByteArray32.convert(chars.mkString.getBytes).getUnsafe))
    Gen.listOfN(size, Arbitrary.arbitrary[(KeySegment, ByteArray32)]).map(_.toMap)
  }

  /** Return data for generating a tree with the longest possibility branch. */
  /*
    [00]PTR: prefix = empty, ptr =>
        [00]PTR: prefix = empty, ptr =>
        ...
                    [00]PTR: prefix = empty, ptr =>
                       [00]LEAF: prefix = empty, data = 0000...0080
                       [01]LEAF: prefix = empty, data = 0000...007F
                    [01]LEAF: prefix = empty, data = 0000...007E
        ...
        [01]LEAF: prefix = empty, data = 0000...0002
     [01]LEAF: prefix = empty, data = 0000...0001
   */
  val longestBranch: Map[KeySegment, ByteArray32] = {
    def createBA(size: Int, lastByte: Byte): ByteArray =
      ByteArray(List.fill(size - 1)(0x00.toByte) :+ lastByte)
    def createBA32(lastByte: Byte)                     = ByteArray32.convert(createBA(32, lastByte)).getUnsafe

    val maximumKeyLength = 127 // TODO: Add in KeySegment as constant

    val branchData: Seq[(KeySegment, ByteArray32)] =
      (0 to maximumKeyLength).map(level => (KeySegment(createBA(level, 0x01.toByte)), createBA32(level.toByte)))
    val lastLeaf                                   = (KeySegment(createBA(maximumKeyLength, 0x00.toByte)), createBA32((maximumKeyLength + 1).toByte))
    (branchData :+ lastLeaf).toMap
  }

  def withRadixTree(
    f: (
      RadixTreeImpl[IO],
      KeyValueTypedStore[IO, ByteArray32, ByteArray],
      ByteArray32 => IO[(ByteArray, Seq[ByteArray32])],
    ) => IO[Unit],
  ): Unit = {
    val store                         = InMemoryKeyValueStore[IO]()
    val typedStore                    = store.toByteArrayTypedStore(RadixHistory.kCodec, RadixHistory.vCodec)
    val radixTreeImpl                 = new RadixTreeImpl[IO](typedStore)
    def getNext(nodePtr: ByteArray32) = radixTreeImpl.getNextForTraverse(nodePtr).map(_.get)
    f(radixTreeImpl, typedStore, getNext).timeout(20.seconds).unsafeRunSync()
  }
}
