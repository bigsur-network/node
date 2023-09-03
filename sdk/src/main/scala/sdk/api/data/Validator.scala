package sdk.api.data

final case class Validator(
  publicKey: Array[Byte], // Unique index
  http: String,
) {
  override def equals(obj: Any): Boolean = obj match {
    case that: Validator =>
      this.publicKey.sameElements(that.publicKey) && this.http == that.http
    case _               => false
  }
}
