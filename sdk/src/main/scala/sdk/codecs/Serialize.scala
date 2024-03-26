package sdk.codecs

/**
 * Type class to provide a binary representation of a value.
 * 
 * {{{ This is the base for binary protocol. }}}
 * 
 * Any type that is supposed to be transmitted over the wire, or have a digest or stored on chain should have an
 * instance of this type class.
 *
 * Automatic derivation of instances is provided by the [[sdk.serialize.auto]] in macro project.
 * */
trait Serialize[F[_], A] {
  def write(x: A): PrimitiveWriter[F] => F[Unit]
  def read: PrimitiveReader[F] => F[A]
}
