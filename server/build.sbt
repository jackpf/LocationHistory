ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.7.0"

lazy val versions = new {
  val sharedProtos = "0.1.0-SNAPSHOT"
  val scalapb = "0.11.20"
  val grpc = "1.78.0"
  val scopt = "4.1.0"
  val logback = "1.5.23"
  val slf4j = "2.0.17"
  val specs2 = "5.7.0"
  val mockito = "5.21.0"
  val sqlite = "3.51.1.0"
  val scalasql = "0.2.3"
}

lazy val IntegrationTest = config("it") extend Test

lazy val root = (project in file("."))
  .configs(IntegrationTest)
  .settings(
    name := "server",
    inConfig(IntegrationTest)(Defaults.testSettings),
    libraryDependencies ++= Seq(
      "com.jackpf.locationhistory" %% "shared" % versions.sharedProtos,
      "com.thesamet.scalapb" %% "scalapb-runtime" % versions.scalapb,
      "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % versions.scalapb,
      "io.grpc" % "grpc-netty" % versions.grpc,
      "io.grpc" % "grpc-services" % versions.grpc,
      "com.github.scopt" %% "scopt" % versions.scopt,
      "ch.qos.logback" % "logback-classic" % versions.logback,
      "org.slf4j" % "slf4j-api" % versions.slf4j,
      "org.xerial" % "sqlite-jdbc" % versions.sqlite,
      "com.lihaoyi" %% "scalasql-namedtuples" % versions.scalasql
    ),
    // Test Dependencies
    libraryDependencies ++= Seq(
      "org.specs2" %% "specs2-core" % versions.specs2 % "test,it",
      "org.mockito" % "mockito-core" % versions.mockito % "test,it"
    ),
    assembly / assemblyMergeStrategy := {
      case x if x.endsWith("module-info.class")            => MergeStrategy.discard
      case x if x.endsWith("io.netty.versions.properties") => MergeStrategy.discard
      case x                                               =>
        val oldStrategy = (assembly / assemblyMergeStrategy).value
        oldStrategy(x)
    },
    ThisBuild / scalacOptions ++= Seq(
      "-deprecation",
      "-feature",
      "-unchecked",
      "-Xfatal-warnings",
      "-Wsafe-init",
      "-Wvalue-discard",
      "-Wunused:all",
      "-Xcheck-macros",
      "-source:future",
      "-no-indent",
      "-language:implicitConversions",
      // Needed by scalasql
      "-language:adhocExtensions"
    ),
    // Disable discarding non-unit values for tests
    Test / scalacOptions += "-Wconf:msg=discarded:s",
    ThisBuild / scalafmtOnCompile := true,
    ThisBuild / semanticdbEnabled := true,
    ThisBuild / semanticdbVersion := scalafixSemanticdb.revision,
    IntegrationTest / parallelExecution := false,
    IntegrationTest / testOptions += Tests
      .Argument(TestFrameworks.Specs2, "sequential")
  )
