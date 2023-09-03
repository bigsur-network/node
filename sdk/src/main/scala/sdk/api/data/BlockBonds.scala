package sdk.api.data

import cats.kernel.Eq

final case class BlockBonds(blockId: Long, bondId: Long)

object BlockBonds {
  implicit val bondEq: Eq[BlockBonds] = Eq.fromUniversalEquals
}
