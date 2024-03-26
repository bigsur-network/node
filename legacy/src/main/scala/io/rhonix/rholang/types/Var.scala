package io.rhonix.rholang.types

final case class BoundVarN(idx: Int) extends VarN
object BoundVarN { def apply(value: Int): BoundVarN = new BoundVarN(value) }

final case class FreeVarN(idx: Int) extends VarN
object FreeVarN { def apply(value: Int): FreeVarN = new FreeVarN(value) }

object WildcardN extends VarN
