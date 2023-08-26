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
  val scalacheckShapeless = "com.github.alexarchambault" %% "scalacheck-shapeless_1.16" % "1.3.1"   % Test
  val scalatest           = "org.scalatest"              %% "scalatest"                 % "3.2.15" // cross CrossVersion.for3Use2_13
  val scalatest_ce        =
    "org.typelevel" %% "cats-effect-testing-scalatest" % "1.4.0" % Test // cross CrossVersion.for3Use2_13
  val mockito      = "org.mockito"   %% "mockito-scala-cats" % "1.17.12" % Test
  val scalacheck_e = "org.typelevel" %% "scalacheck-effect"  % "1.0.4"   % Test
  val scalatestScalacheck  = "org.scalatestplus" %% "scalacheck-1-17" % "3.2.16.0" % Test

  // Diagnostics
  val kamonBundle           = "io.kamon" %% "kamon-bundle"   % "2.6.1"
  val kamonInfluxDbReporter = "io.kamon" %% "kamon-influxdb" % "2.6.0"
  val kamonJaegerReporter   = "io.kamon" %% "kamon-jaeger"   % "2.6.0"

  val http4sNetty = "org.http4s" %% "http4s-netty-server" % "0.5.9"
  val http4sBlaze = "org.http4s" %% "http4s-blaze-server" % "0.23.14"
  val http4sDSL   = "org.http4s" %% "http4s-dsl"          % "0.23.23"
  val circeCodec  = "org.http4s" %% "http4s-circe"        % "0.23.23"

  // Database
  val embeddedPostgres           = "io.zonky.test"     % "embedded-postgres" % "2.0.4"  % Test
  val junitJupiter               = "org.junit.jupiter" % "junit-jupiter-api" % "5.10.0" % Test
  val postgresql                 = "org.postgresql"    % "postgresql"        % "42.6.0"
  val squeryl                    = "org.squeryl"      %% "squeryl"           % "0.10.0"
  val liquibase4s: Seq[ModuleID] = Seq(
    "io.github.liquibase4s" %% "liquibase4s-core"        % "1.0.0",
    "io.github.liquibase4s" %% "liquibase4s-cats-effect" % "1.0.0",
  )

  val common = Seq(catsCore, catsEffect, fs2Core)

  val diagnostics = Seq(kamonBundle, kamonInfluxDbReporter, kamonJaegerReporter)

  val http4s = Seq(http4sNetty, http4sDSL, circeCodec, http4sBlaze)

  val tests = Seq(scalatest, scalatest_ce, mockito, scalacheck_e, scalacheckShapeless, scalatestScalacheck)

  val dbLibs = Seq(embeddedPostgres, postgresql, squeryl, junitJupiter) ++ liquibase4s
}
