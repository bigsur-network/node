package sdk.api.data

final case class Justification(
  id: Long,
  validator: Validator,
  lastestBlock: Block,
)
