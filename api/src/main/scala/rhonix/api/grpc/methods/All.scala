package rhonix.api.grpc.methods

import io.grpc.ServerServiceDefinition
import rhonix.api.grpc.methods.Balances.{balancesMethodDescriptor, balancesServerCallHandler}

object All {

  // Service (API) description, mapping of methods specification with handlers
  def apply(serviceName: String, getBalance: String => Long): ServerServiceDefinition = ServerServiceDefinition
    .builder(serviceName)
    .addMethod(balancesMethodDescriptor, balancesServerCallHandler(getBalance))
    .build()
}
