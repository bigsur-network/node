package sdk.api

trait ReadAPI {
  def find[ID, T](id: ID, proj: ID => T): Option[T]
}
