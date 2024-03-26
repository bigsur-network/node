import BNFC.*
import Dependencies.*
import Secp256k1.*
import sbtassembly.MergeStrategy

val scala3Version       = "3.3.0"
val scala2Version       = "2.13.10"
lazy val commonSettings = Seq(
  version           := "0.1.0-SNAPSHOT",
  scalafmtOnCompile := !sys.env.contains("CI"), // Format on compile, disable in CI environments

  // Java 17+ has more restrictive policy for access to JDK internals.
  // Without the following java options liquibase library does not work properly
  // and tests fail with "java.base does not "opens java.lang" to unnamed module @4b8f7a19"
  // Please see https://dev.java/learn/modules/add-exports-opens/
  // NOTE: this does not help with running tests in IntelliJ, so
  // to run such tests in IntelliJ this argument has to be added explicitly.
  javaOptions ++= Seq(
    "--add-opens=java.base/java.lang=ALL-UNNAMED",
    "--add-opens=java.base/java.nio=ALL-UNNAMED",
    "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED",
  ),
  Test / fork                            := true,
  Test / parallelExecution               := false,
  Test / testForkedParallel              := false,
  Compile / packageDoc / publishArtifact := false,
)

lazy val settingsScala3 = commonSettings ++ Seq(
  scalaVersion := scala3Version,
  scalacOptions ++= CompilerOptions.DEFAULTS,
  Compile / compile / wartremoverErrors ++= WartsSettings.DEFAULTS,
)

lazy val settingsScala2 = commonSettings ++ Seq(
  scalaVersion := scala2Version,
  // Enables Scala2 project to depend on Scala3 projects
  scalacOptions += "-Ytasty-reader",
  scalacOptions ++= CompilerOptions.DEFAULTS_SCALA_2,
  Compile / compile / wartremoverErrors ++= WartsSettings.DEFAULTS_SCALA_2.filterNot(Seq(Wart.SeqApply).contains),
)

lazy val gorkiNode = (project in file("."))
  .settings(commonSettings*)
  .aggregate(sdk, weaver, dproc, db, node, legacy, sim, diag, macros, secp256k1)

lazy val sdk = (project in file("sdk"))
//  .settings(settingsScala3*) // Not supported in IntelliJ Scala plugin
  .settings(settingsScala2*)
  .settings(libraryDependencies ++= common ++ dbLibs ++ tests ++ log :+ protobuf :+ bouncyProvCastle :+ magnolia1)

// Database interfaces implementation
lazy val db = (project in file("db"))
  .settings(settingsScala2*)
  .settings(libraryDependencies ++= Seq(catsCore, catsEffect) ++ dbLibs ++ tests ++ log)
  .dependsOn(sdk)

// Consensus
lazy val weaver = (project in file("weaver"))
//  .settings(settingsScala3*) // Not supported in IntelliJ Scala plugin
  .settings(settingsScala2*)
  .settings(
    libraryDependencies ++= common ++ tests,
  )
  .dependsOn(sdk)

// Node logic
lazy val dproc = (project in file("dproc"))
//  .settings(settingsScala3*) // Not supported in IntelliJ Scala plugin
  .settings(settingsScala2*)
  .settings(
    libraryDependencies ++= common ++ tests,
  )
  .dependsOn(sdk, weaver, diag)

// Node implementation
lazy val node = (project in file("node"))
//  .settings(settingsScala3*) // Not supported in IntelliJ Scala plugin
  .settings(settingsScala2*)
  .settings(
    libraryDependencies ++= common ++ Seq(
      protobuf,
      grpc,
      grpcNetty,
    ) ++ tests ++ log ++ http4s ++ endpoints4s :+ lmdbjava :+ enumeratum :+ pureConfig,
    resolvers ++=
      // for embedded InfluxDB
      Resolver.sonatypeOssRepos("releases") ++
        Resolver.sonatypeOssRepos("snapshots"),
    buildInfoKeys    := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion, git.gitHeadCommit),
    buildInfoPackage := "node",

    // Docker
    Compile / mainClass := Some("node.Main"),
    dockerBaseImage     := "azul/zulu-openjdk:18-jre-latest",
    dockerEntrypoint    := Seq(
      s"bin/${(Docker / executableScriptName).value}",
      "-J--add-opens=java.base/java.lang=ALL-UNNAMED",
      "-J--add-opens=java.base/java.nio=ALL-UNNAMED",
      "-J--add-opens=java.base/sun.nio.ch=ALL-UNNAMED",
    ),
  )
  .enablePlugins(JavaAppPackaging, BuildInfoPlugin)
  .dependsOn(
    sdk % "compile->compile;test->test",
    weaver,
    dproc,
    diag,
    db  % "compile->compile;test->test",
    secp256k1,
    macros,
  )

// Diagnostics
lazy val diag = (project in file("diag"))
  //  .settings(settingsScala3*) // Not supported in IntelliJ Scala plugin
  .settings(settingsScala2*)
  .settings(
    libraryDependencies ++= common ++ tests ++ diagnostics,
    // for embedded InfluxDB
    resolvers ++= Resolver.sonatypeOssRepos("releases"),
  )
  .dependsOn(sdk % "compile->compile;test->test")

// TODO this is commented out since JmhPlugin messes up with compile paths and IDEA doesn't like it
// lazy val bench = (project in file("bench"))
//  //  .settings(settingsScala3*) // Not supported in IntelliJ Scala plugin
//  .settings(settingsScala2*)
//  .enablePlugins(JmhPlugin)
//  .settings(
//    libraryDependencies ++= common ++ Seq(protobuf, grpc, grpcNetty) ++ tests,
//  )
//  .dependsOn(sdk, weaver)

lazy val sim = (project in file("sim"))
  //  .settings(settingsScala3*) // Not supported in IntelliJ Scala plugin
  .settings(settingsScala2*)
  .settings(
    libraryDependencies ++= common :+ embedPgsql,
    version                          := "0.1.0-SNSHOT",
    assembly / mainClass             := Some("sim.NetworkSim"),
    assembly / assemblyJarName       := "sim.jar",
    assembly / assemblyMergeStrategy := {
      case PathList("reference.conf")    => MergeStrategy.concat
      case PathList("META-INF", xs @ _*) => MergeStrategy.discard
      case x                             => MergeStrategy.first
    },
  )
  .dependsOn(node, db, diag)

// Legacy implementation (rholang + rspace).
// This also contains new code with rholang implementation, under namespace io.rhonix.
lazy val legacy = (project in file("legacy"))
  .settings(settingsScala2*)
  .settings(bnfcSettings*)
  .settings(
    scalacOptions ~= { options =>
      options.filterNot(Set("-Xfatal-warnings", "-Ywarn-unused:imports")) ++ Seq(
        "-Xlint:-strict-unsealed-patmat",
        "-Xnon-strict-patmat-analysis",
        "-Wconf:cat=deprecation:ws",   // suppress deprecation warnings
        "-Xlint:-missing-interpolator",// Disable false positive strings containing ${...}
      ) ++ Seq(
        "-Xlint:-strict-unsealed-patmat",
        "-Xnon-strict-patmat-analysis",
      ) // TODO Matching the rholang object should be always exhaustive. Remove when done.
    },
    Compile / compile / wartremoverErrors ~= {
      _.filterNot(Seq(Wart.SeqApply, Wart.Throw, Wart.Var, Wart.SeqUpdated).contains)
    },
    libraryDependencies ++= common ++ tests ++ legacyLibs,
    resolvers += ("jitpack" at "https://jitpack.io"),
  )
  .dependsOn(sdk, macros, secp256k1) // depends on new rholang implementation

// Macro implementation should be compiled before macro application
// https://stackoverflow.com/questions/75847326/macro-implementation-not-found-scala-2-13-3
lazy val macros = (project in file("macros"))
  .settings(settingsScala2*)
  .settings(libraryDependencies ++= Seq(scalaReflect(scala2Version), kindProjector))
  .dependsOn(sdk)

lazy val secp256k1 = (project in file("secp256k1"))
  .settings(settingsScala2*)
  .settings(
    libraryDependencies ++= common ++ log :+ bcprov,
    // this is quite hacky way to pull native libraries
    pullNative := {
      val log        = streams.value.log
      val pullCached = FileFunction.cached(
        // this string does not matter, just has to some folder that is supposed to be created and
        // looked up to see if cache exists
        streams.value.cacheDirectory / "qXr7LbNp",
        inStyle = FilesInfo.hash,
        outStyle = FilesInfo.exists,
      ) { (in: Set[File]) =>
        log.info("Missing Secp256k1 native library, downloading...")
        // This is the main function that does download native libs into managed resource
        pullSecp256k1(in.head)
      }(Set((Compile / resourceManaged).value))
      // Returning generated files paths is important so they are copied by sbt to classes folder
      pullCached.toSeq
    },
    // NOTE: this is not called on `compile` but when tests are called or on assembly
    Compile / resourceGenerators += pullNative.taskValue,
  )
  .dependsOn(sdk % "compile->compile;test->test")
