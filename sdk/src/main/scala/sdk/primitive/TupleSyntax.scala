package sdk.primitive

trait TupleSyntax {
  implicit def sdkSyntaxTuple2[A](x: (A, A)): Tuple2Ops[A] = new Tuple2Ops[A](x)

  implicit def sdkSyntaxTuple3[A](x: (A, A, A)): Tuple3Ops[A] = new Tuple3Ops[A](x)

  implicit def sdkSyntaxTuple4[A](x: (A, A, A, A)): Tuple4Ops[A] = new Tuple4Ops[A](x)
}

final class Tuple2Ops[A](private val x: (A, A)) extends AnyVal {
  def nmap[B](f: A => B): (B, B) = (f(x._1), f(x._2))
}

final class Tuple3Ops[A](private val x: (A, A, A)) extends AnyVal {
  def nmap[B](f: A => B): (B, B, B) = (f(x._1), f(x._2), f(x._3))
}

final class Tuple4Ops[A](private val x: (A, A, A, A)) extends AnyVal {
  def nmap[B](f: A => B): (B, B, B, B) = (f(x._1), f(x._2), f(x._3), f(x._3))
}
