ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.7"

lazy val versions = new {
  val sharedProtos = "0.1.0-SNAPSHOT"
  val scalapb = "0.11.20"
  val grpcNetty = "1.78.0"
  val scopt = "4.1.0"
  val logback = "1.5.23"
  val slf4j = "2.0.17"
  val munit = "1.2.1"
  val mockito = "5.21.0"
}

lazy val root = (project in file("."))
  .settings(
    name := "server",
    libraryDependencies ++= Seq(
      "com.jackpf.locationhistory" %% "shared" % versions.sharedProtos,
      "com.thesamet.scalapb" %% "scalapb-runtime" % versions.scalapb,
      "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % versions.scalapb,
      "io.grpc" % "grpc-netty" % versions.grpcNetty,
      "com.github.scopt" %% "scopt" % versions.scopt,
      "ch.qos.logback" % "logback-classic" % versions.logback,
      "org.slf4j" % "slf4j-api" % versions.slf4j
    ),
    // Test Dependencies
    libraryDependencies ++= Seq(
      "org.scalameta" %% "munit" % versions.munit % "test,it",
      "org.mockito" % "mockito-core" % versions.mockito % "test,it"
    ),
    ThisBuild / scalacOptions ++= Seq(
      "-deprecation",
      "-feature",
      "-unchecked",
      "-Xfatal-warnings",
      "-Ysafe-init",
      "-Wvalue-discard",
      "-Wunused:all",
      "-Xcheck-macros",
      "-Wconf:any:warning-verbose",
      "-source:future",
      "-no-indent"
    ),
    ThisBuild / scalafmtOnCompile := true,
    ThisBuild / semanticdbEnabled := true,
    ThisBuild / semanticdbVersion := scalafixSemanticdb.revision
  )
