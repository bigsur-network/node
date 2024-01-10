package sdk.data

import sdk.codecs.Digest
import sdk.primitive.ByteArray

/**
 * Deploy is a state diff with id.
 * */
final case class BalancesDeploy(id: ByteArray, body: BalancesDeployBody) {
  override def equals(obj: Any): Boolean = obj match {
    case BalancesDeploy(id, _) => id == this.id
    case _                     => false
  }

  override def hashCode(): Int = id.hashCode()
}

object BalancesDeploy {
  implicit val ordDeploy: Ordering[BalancesDeploy] = Ordering.by(_.id)

  def apply(state: BalancesDeployBody)(implicit d: Digest[BalancesDeployBody]): BalancesDeploy = {
    val id = d.digest(state)
    new BalancesDeploy(id, state)
  }
}
