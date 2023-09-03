package sdk.api

// Package containing data types used in the API
package object data {
  type BlockId     = Array[Byte] // hash of the block
  type ValidatorId = Array[Byte] // public key of the validator
  type DeployId    = Array[Byte] // signature of the deploy
}
