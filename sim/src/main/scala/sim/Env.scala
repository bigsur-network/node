package sim

import cats.Applicative
import cats.effect.kernel.{Async, Temporal}
import cats.effect.std.Random
import cats.effect.{Ref, Sync}
import cats.syntax.all.*
import dproc.DProc
import dproc.data.Block
import fs2.{Pipe, Stream}
import sim.Sim.*
import weaver.data.{ConflictResolution, FinalData}

import scala.collection.concurrent.TrieMap
import scala.concurrent.duration.{Duration, DurationInt}

/** Mocks for real world things. */
object Env {
  val rnd = scala.util.Random

  type TupleSpace = Map[Int, Long]
  type Diff       = TupleSpace

  def mkTx = {
    val from = rnd.between(0, 20)
    val to   = rnd.between(0, 20)
    val amt  = rnd.between(0, 100).toLong
    Map(from -> -amt, to -> amt)
  }

  // send from x to y an amount
  final case class Tx(id: Long, diff: Diff)
  val rndTx = Tx(rnd.nextLong(), mkTx)

  /** Shared block store across simulation. */
  val blocks = TrieMap.empty[M, Block.WithId[M, S, T]]

  /**
   * Shared tx store across simulation.
   * Transactions here can also be thought of as a tuple spaces since tx lead
   * to a state diff which is a tuple space.
   */
  val utxos = TrieMap.empty[Long, Tx]

  def randomTGen[F[_]: Random] = Random[F].nextString(10)

  def broadcast[F[_]: Temporal](
    peers: List[(String, DProc[F, M, T])],
    time: Duration,
  ): Pipe[F, M, Unit] = _.evalMap { x =>
    Temporal[F].sleep(time) >> peers.traverse { case (_, dProc) =>
      dProc.acceptMsg(x)
    }.void
  }

  // ids for messages of particular sender (no equivocation)
  // instead of using cryptographic hashing function
  def dummyIds(sender: S): LazyList[String] =
    LazyList.unfold(Map.empty[S, Int]) { acc =>
      val next   = acc.getOrElse(sender, 0) + 1
      val newAcc = acc + (sender -> next)
      val id     = s"$sender|-$next"
      (id, newAcc).some
    }

  def dummyExeEngine[F[_]: Async](genesisState: FinalData[S], exeTime: Duration, stateReadTime: Duration) =
    new DProc.ExeEngine[F, M, S, T] {
      // bonds and shard config always the same
      override def consensusData(fringe: Set[M]): F[FinalData[S]] = Temporal[F].sleep(stateReadTime).as(genesisState)

      // merge (just a fold of monoid) + execution (application of a merged diff onto the fringe + tx execution)
      override def execute(fringe: Set[M], toMerge: Iterable[T], toExecute: Iterable[T]): F[Boolean] = Sync[F].defer {
        val _ = toMerge.toList.combineAll // merge pre state
        Temporal[F].sleep(exeTime).as(true) // and execute
      }
    } ->
      // nothing conflicts
      new sdk.consensus.Relation[F, T] {
        override def conflicts(l: T, r: T): F[Boolean] = false.pure
        override def depends(x: T, on: T): F[Boolean]  = false.pure
      }

  def TPS[F[_]: Async]: Pipe[F, ConflictResolution[T], Int] = { (in: Stream[F, ConflictResolution[T]]) =>
    in.flatMap(x => Stream.emits(x.accepted.toList)).through(measurePerSec)
  }

  def measurePerSec[F[_]: Async, I]: Pipe[F, I, Int] = {
    val acc = Ref.unsafe[F, Int](0)
    val out = Stream.eval(acc.getAndUpdate(_ => 0)).delayBy(1.second).repeat
    (in: Stream[F, I]) => out concurrently in.evalTap(_ => acc.update(_ + 1))
  }

  def crWithLog[F[_]: Applicative]: Pipe[F, ConflictResolution[T], ConflictResolution[T]] =
    (in: Stream[F, ConflictResolution[T]]) =>
      in.mapAccumulate(0) { case (acc, cr) => (acc + cr.accepted.size) -> cr }
        .evalTap(x => println(s"Finalized ${x._1} deploys").pure[F])
        .map(_._2)

}
