package sdk.data

import sdk.primitive.ByteArray

final case class Deploy(
  sig: ByteArray,        // deploy signature
  deployerPk: ByteArray, // deployer public key
  shardName: String,     // unique name of a shard
  program: String,       // code of the program
  phloPrice: Long,       // price offered for phlogiston
  phloLimit: Long,       // limit offered for execution
  nonce: Long,           // nonce of a deploy // TODO: change nonce to validAfterBlockNumber
)
