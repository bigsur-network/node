package sdk.store

import cats.Parallel
import cats.effect.{Async, Resource}
import cats.syntax.all.*
import sdk.codecs.Codec
import sdk.history.ByteArray32
import sdk.history.instances.RadixHistory
import sdk.history.instances.RadixHistory.EmptyRootHash
import sdk.primitive.ByteArray
import sdk.syntax.all.*

/**
  * Shard store: history + values.
  */
final case class HistoryWithValues[F[_], A](
  history: RadixHistory[F],
  values: KeyValueTypedStore[F, ByteArray32, A],
)

object HistoryWithValues {

  private val HistoryLabel = "history"
  private val ValuesLabel  = "data"

  def apply[F[_]: Async: Parallel, A](
    kvStoreManager: KeyValueStoreManager[F],
  )(implicit hash32: Array[Byte] => ByteArray32, aCodec: Codec[A, ByteArray]): Resource[F, HistoryWithValues[F, A]] =
    kvStoreManager.asResource.flatMap { kvStoreManager =>
      Resource.eval {
        for {
          historyStore <- kvStoreManager.store(HistoryLabel)
          valuesStore  <- kvStoreManager.store(ValuesLabel)
          history      <- sdk.history.History.create(EmptyRootHash, historyStore)
          values        = valuesStore.toByteArrayTypedStore[ByteArray32, A](ByteArray32.codec, aCodec)
        } yield HistoryWithValues(history, values)
      }
    }
}
