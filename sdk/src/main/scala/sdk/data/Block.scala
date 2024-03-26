package sdk.data

import sdk.primitive.ByteArray

final case class Block(
  version: Int,    // protocol versions
  hash: ByteArray, // strong hash of block content

  // Signature
  sigAlg: String,       // signature of a hash
  signature: ByteArray, // signing algorithm

  // On Chain state
  finalStateHash: ByteArray, // proof of final state
  postStateHash: ByteArray,  // proof of pre state

  // pointers to inner data
  validatorPk: ByteArray,           // validator public key
  shardName: String,                // name of a shard
  justificationSet: Set[ByteArray], // justification set
  seqNum: Long,                     // sequence number
  offencesSet: Set[ByteArray],      // offences set

  bondsMap: Map[ByteArray, Long],      // bonds map
  finalFringe: Set[ByteArray],         // final fringe set
  execDeploySet: Set[ByteArray],       // deploy set executed in the block
  mergeDeploySet: Set[ByteArray],      // deploy set merged into pre state
  dropDeploySet: Set[ByteArray],       // deploy set rejected from pre state
  mergeDeploySetFinal: Set[ByteArray], // deploy set finally accepted
  dropDeploySetFinal: Set[ByteArray],  // deploy set finally rejected
)

object Block {
  def apply(b: sdk.api.data.Block): Block = new sdk.data.Block(
    b.version,
    ByteArray(b.hash),
    b.signatureAlg,
    ByteArray(b.signature),
    ByteArray(b.finStateHash),
    ByteArray(b.postStateHash),
    ByteArray(b.sender),
    b.shardId,
    b.justifications.map(ByteArray(_)),
    b.seqNum,
    Set(),
    b.bonds.map(x => ByteArray(x.validator) -> x.stake).toMap,
    Set(),
    b.deploys.map(ByteArray(_)),
    Set(),
    Set(),
    Set(),
    Set(),
  )
}
