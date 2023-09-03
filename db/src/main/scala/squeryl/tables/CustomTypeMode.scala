package squeryl.tables

import org.squeryl.dsl.{CanCompare, TByteArray}

object CustomTypeMode extends org.squeryl.PrimitiveTypeMode {
  // custom types, etc, go here
  implicit val canCompareByteArray: CanCompare[TByteArray, TByteArray] = new CanCompare[TByteArray, TByteArray]
}
