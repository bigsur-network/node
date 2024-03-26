package slick.api.data

final case class Block(
  version: Int,                 // protocol versions
  hash: Array[Byte],            // strong hash of block content
  sigAlg: String,               // signature of a hash
  signature: Array[Byte],       // signing algorithm
  finalStateHash: Array[Byte],  // proof of final state
  postStateHash: Array[Byte],   // proof of pre state
  validatorPk: Array[Byte],     // validator public key
  shardName: String,            // name of a shard
  justificationSet: SetData,    // justification set
  seqNum: Long,                 // sequence number
  offencesSet: SetData,         // offences set
  bondsMap: BondsMapData,       // bonds map
  finalFringe: SetData,         // final fringe set
  execDeploySet: SetData,       // deploy set executed in the block
  mergeDeploySet: SetData,      // deploy set merged into pre state
  dropDeploySet: SetData,       // deploy set rejected from pre state
  mergeDeploySetFinal: SetData, // deploy set finally accepted
  dropDeploySetFinal: SetData,  // deploy set finally rejected
)

final case class SetData(
  hash: Array[Byte],
  data: Seq[Array[Byte]],
)

final case class BondsMapData(
  hash: Array[Byte],
  data: Seq[(Array[Byte], Long)],
)
