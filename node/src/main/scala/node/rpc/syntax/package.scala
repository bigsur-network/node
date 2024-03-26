package node.rpc

import node.rpc.syntax.GrpcClientSyntax

package object syntax {
  object all extends AllSyntaxRpc
}

trait AllSyntaxRpc extends GrpcClientSyntax
