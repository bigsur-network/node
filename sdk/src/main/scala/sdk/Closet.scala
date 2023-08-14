package sdk

object Closet {
  //  def waitUntil[F[_]: Monad: Timer](condition: F[Boolean]): F[Unit] =
  //    ().pure[F].tailRecM { _ =>
  //      condition.map { r =>
  //        if (r) ().asRight[F[Unit]] else Timer[F].sleep(50.milliseconds).asLeft[Unit]
  //      }
  //    }
  //
  //  def idealBCastAwaitingParents[F[_]: Concurrent: Timer](
  //      peers: List[(String, DProc[F, M, S, T])]
  //  ): Pipe[F, Msg.WithId[M, S, T], Unit] =
  //    _.evalTap(_ => Timer[F].sleep(100.milliseconds)).through(idealBroadcast(peers))

  //  def deployToRandom[F[_]: FlatMap: Random, M](sinks: List[M => F[Unit]]): Pipe[F, M, Unit] =
  //    _.evalMap(m => Random[F].nextIntBounded(sinks.size).flatMap(r => sinks(r)(m)))

  //  def transactionStream[F[_]: Temporal: Random]: Stream[F, String] =
  //    Stream.awakeEvery[F](2.seconds).evalMap { _ =>
  //      val delayF =
  //        Random[F].nextIntBounded(100).map(DurationInt(_).milliseconds).flatTap(Temporal[F].sleep)
  //      delayF.flatMap(_ => randomTGen)
  //    }

}
