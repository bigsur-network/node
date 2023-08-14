import sbt.*

object Dependencies {
  // Core dependencies
  val catsCore   = "org.typelevel" %% "cats-core"   % "2.9.0" // cross CrossVersion.for3Use2_13
  val mouse      = "org.typelevel" %% "mouse"       % "1.2.1" // cross CrossVersion.for3Use2_13
  val catsEffect = "org.typelevel" %% "cats-effect" % "3.5.0" // cross CrossVersion.for3Use2_13
  val fs2Core    = "co.fs2"        %% "fs2-core"    % "3.7.0" // cross CrossVersion.for3Use2_13

  // Network communication
  val grpc      = "io.grpc" % "grpc-core"  % "1.53.0"
  val grpcNetty = "io.grpc" % "grpc-netty" % "1.53.0"

  // LEGACY dependencies of imported projects
  val protobuf = "com.google.protobuf" % "protobuf-java" % "3.22.2"

  // Testing frameworks
  val scalatest    = "org.scalatest" %% "scalatest" % "3.2.15" % Test // cross CrossVersion.for3Use2_13
  val scalatest_ce =
    "org.typelevel" %% "cats-effect-testing-scalatest" % "1.4.0" % Test // cross CrossVersion.for3Use2_13
  val mockito      = "org.mockito"   %% "mockito-scala-cats" % "1.17.12" % Test
  val scalacheck_e = "org.typelevel" %% "scalacheck-effect"  % "1.0.4"   % Test

  // Diagnostics
  val kamon                 = "io.kamon" %% "kamon-core"        % "2.6.3"
  val kamonStatus           = "io.kamon" %% "kamon-status-page" % "2.6.3"
  val kamonInfluxDbReporter = "io.kamon" %% "kamon-influxdb"    % "2.6.0"
  val kamonZipkinReporter   = "io.kamon" %% "kamon-jaeger"      % "2.6.0"

  val common = Seq(catsCore, catsEffect, fs2Core)

  val diagnostics = Seq(kamon, kamonStatus, kamonInfluxDbReporter, kamonZipkinReporter)

  val tests = Seq(scalatest, scalatest_ce, mockito, scalacheck_e)
}
