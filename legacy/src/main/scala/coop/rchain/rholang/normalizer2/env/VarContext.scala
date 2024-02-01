package coop.rchain.rholang.normalizer2.env

import coop.rchain.rholang.interpreter.compiler.SourcePosition

final case class VarContext[T](index: Int, typ: T, sourcePosition: SourcePosition)