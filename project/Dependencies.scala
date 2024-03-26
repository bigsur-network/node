import sbt.*

object Dependencies {
  // Core dependencies
  val catsCore   = "org.typelevel" %% "cats-core"   % "2.9.0" // cross CrossVersion.for3Use2_13
  val mouse      = "org.typelevel" %% "mouse"       % "1.2.1" // cross CrossVersion.for3Use2_13
  val catsEffect = "org.typelevel" %% "cats-effect" % "3.5.0" // cross CrossVersion.for3Use2_13
  val fs2Core    = "co.fs2"        %% "fs2-core"    % "3.7.0" // cross CrossVersion.for3Use2_13

  // Added to support legacy code. But useful library in general.
  val kindProjector = compilerPlugin(
    "org.typelevel" %% "kind-projector" % "0.13.2" cross CrossVersion.full,
  )

  // Network communication
  val grpc      = "io.grpc" % "grpc-core"  % "1.61.0"
  val grpcNetty = "io.grpc" % "grpc-netty" % "1.61.0"

  // LEGACY dependencies of imported projects
  val protobuf           = "com.google.protobuf"         % "protobuf-java"    % "3.22.2"
  val scalapbRuntimeLib  = "com.thesamet.scalapb"       %% "scalapb-runtime"  % "0.11.13"
  val jaxb               = "javax.xml.bind"              % "jaxb-api"         % "2.3.1"
  val scalapbCompiler    = "com.thesamet.scalapb"       %% "compilerplugin"   % "0.11.13"
  val magnolia           = "com.propensive"             %% "magnolia"         % "0.17.0"
  val bouncyProvCastle   = "org.bouncycastle"            % "bcprov-jdk15on"   % "1.70"
  val bouncyPkixCastle   = "org.bouncycastle"            % "bcpkix-jdk15on"   % "1.70"
  val guava              = "com.google.guava"            % "guava"            % "31.1-jre"
  val scodecCore         = "org.scodec"                 %% "scodec-core"      % "1.11.10"
  val scodecCats         = "org.scodec"                 %% "scodec-cats"      % "1.2.0"
  val scodecBits         = "org.scodec"                 %% "scodec-bits"      % "1.1.37"
  val shapeless          = "com.chuusai"                %% "shapeless"        % "2.3.10"
  val lz4                = "org.lz4"                     % "lz4-java"         % "1.8.0"
  val lmdbjava           = "org.lmdbjava"                % "lmdbjava"         % "0.9.0"
  val enumeratum         = "com.beachape"               %% "enumeratum"       % "1.7.2"
  val xalan              = "xalan"                       % "xalan"            % "2.7.3"
  val catsMtl            = "org.typelevel"              %% "cats-mtl-core"    % "0.7.1"
  val catsMtlLaws        = "org.typelevel"              %% "cats-mtl-laws"    % "0.7.1"
  val kalium             = "com.github.rchain"           % "kalium"           % "0.8.1"
  val lightningj         = "org.lightningj"              % "lightningj"       % "0.5.2-Beta"
  val scalacheck         = "org.scalacheck"             %% "scalacheck"       % "1.14.1"
  val scalaLogging       = "com.typesafe.scala-logging" %% "scala-logging"    % "3.9.4"
  val scallop            = "org.rogach"                 %% "scallop"          % "3.3.2"
  val catsEffectLawsTest = "org.typelevel"              %% "cats-effect-laws" % "3.5.0" % "test"
  val apacheCommons      = "org.apache.commons"          % "commons-lang3"    % "3.13.0"
  val legacyLibs         = Seq(
    magnolia,
    guava,
    bouncyProvCastle,
    bouncyPkixCastle,
    protobuf,
    scalapbRuntimeLib,
    scalapbCompiler,
    scodecCore,
    scodecCats,
    scodecBits,
    lz4,
    lmdbjava,
    enumeratum,
    xalan,
    catsMtl,
    catsMtlLaws,
    kalium,
    lightningj,
    shapeless,
    scalacheck,
    scalaLogging,
    scallop,
    catsEffectLawsTest,
    apacheCommons,
  )

  // Testing frameworks
  val scalacheckShapeless = "com.github.alexarchambault" %% "scalacheck-shapeless_1.16" % "1.3.1" % Test
  val scalatest           = "org.scalatest"              %% "scalatest"                 % "3.2.15" // cross CrossVersion.for3Use2_13
  val scalatest_ce        =
    "org.typelevel" %% "cats-effect-testing-scalatest" % "1.4.0" % Test // cross CrossVersion.for3Use2_13
  val mockito             = "org.mockito"       %% "mockito-scala-cats" % "1.17.12"  % Test
  val scalacheck_e        = "org.typelevel"     %% "scalacheck-effect"  % "1.0.4"    % Test
  val scalatestScalacheck = "org.scalatestplus" %% "scalacheck-1-17"    % "3.2.16.0" % Test
  val embedPgsql          = "io.zonky.test"      % "embedded-postgres"  % "2.0.6"

  // Diagnostics
  val kamonBundle           = "io.kamon"    %% "kamon-bundle"         % "2.6.1"
  val kamonInfluxDbReporter = "io.kamon"    %% "kamon-influxdb"       % "2.6.0"
  val kamonJaegerReporter   = "io.kamon"    %% "kamon-jaeger"         % "2.6.0"
  val influxDbClient        = "com.influxdb" % "influxdb-client-java" % "6.10.0"

  // Logging
  val logbackClassic   = "ch.qos.logback"              % "logback-classic" % "1.4.7"
  // val slf4j            = "org.slf4j"                   % "slf4j-api"       % "2.0.5"
  val typesagfeLogging = "com.typesafe.scala-logging" %% "scala-logging"   % "3.9.4"

  // Reflection
  def scalaReflect(scalaVersion: String): ModuleID = "org.scala-lang" % "scala-reflect" % scalaVersion

  // Typeclass derivation
  val magnolia1 = "com.softwaremill.magnolia1_2" %% "magnolia" % "1.1.8"

  // Config
  val pureConfig = "com.github.pureconfig" %% "pureconfig" % "0.17.4"

  // Web
  val http4sNetty  = "org.http4s" %% "http4s-netty-server" % "0.5.9"
  val http4sBlaze  = "org.http4s" %% "http4s-blaze-server" % "0.23.14"
  val http4sDSL    = "org.http4s" %% "http4s-dsl"          % "0.23.23"
  val circeCodec   = "org.http4s" %% "http4s-circe"        % "0.23.23"
  // for auto-derivation of JSON codecs
  val circeGeneric = "io.circe"   %% "circe-generic"       % "0.14.5"
  val circeParser  = "io.circe"   %% "circe-parser"        % "0.14.5"

  val endpointsAlg       = "org.endpoints4s" %% "algebra"             % "1.9.0"
  val endpointsAlgCirce  = "org.endpoints4s" %% "algebra-circe"       % "2.3.0"
  val endpointsAlgJson   = "org.endpoints4s" %% "algebra-json-schema" % "1.9.0"
  val endpointsGeneric   = "org.endpoints4s" %% "json-schema-generic" % "1.10.0"
  val endpointsHttp4s    = "org.endpoints4s" %% "http4s-server"       % "10.1.0"
  val endpointsJsonCirce = "org.endpoints4s" %% "json-schema-circe"   % "2.3.0"
  val endpointsOpenApi   = "org.endpoints4s" %% "openapi"             % "4.4.0"

  // Database
  val junitJupiter         = "org.junit.jupiter"  % "junit-jupiter-api" % "5.10.0" % Test
  val postgresql           = "org.postgresql"     % "postgresql"        % "42.7.1"
  val slick: Seq[ModuleID] = Seq(
    "com.typesafe.slick"                 %% "slick"               % "3.4.1",
    "com.typesafe.slick"                 %% "slick-hikaricp"      % "3.4.1",
    "io.github.nafg.slick-migration-api" %% "slick-migration-api" % "0.9.0",// Migration tool for Slick
  )
  val dbcp2                = "org.apache.commons" % "commons-dbcp2"     % "2.9.0"

  // Cryptography
  val bcprov = "org.bouncycastle" % "bcprov-jdk15on" % "1.68"

  val apacheCommonsIO = "commons-io" % "commons-io" % "2.15.1"

  val common = Seq(catsCore, catsEffect, fs2Core, jaxb, kindProjector, circeGeneric, circeParser, apacheCommonsIO)

  val diagnostics = Seq(kamonBundle, kamonInfluxDbReporter, kamonJaegerReporter, influxDbClient)

  val log = Seq(logbackClassic, typesagfeLogging)

  val http4s      = Seq(http4sNetty, http4sDSL, circeCodec, http4sBlaze)
  val endpoints4s =
    Seq(
      endpointsAlg,
      endpointsAlgCirce,
      endpointsAlgJson,
      endpointsGeneric,
      endpointsHttp4s,
      endpointsJsonCirce,
      endpointsOpenApi,
    )

  val tests = Seq(scalatest, scalatest_ce, mockito, scalacheck_e, scalacheckShapeless, scalatestScalacheck, embedPgsql)

  val dbLibs = Seq(postgresql, junitJupiter, dbcp2) ++ slick
}
