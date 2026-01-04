ThisBuild / organization := "com.jackpf.locationhistory"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.6.2"

lazy val versions = new {
  val grpc = "1.78.0"
  val javaxAnnotation = "1.3.2"
}

lazy val root = (project in file("."))
  .settings(
    name := "shared",
    libraryDependencies ++= Seq(
      "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf",
      "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion,
      // Force grpc version
      "io.grpc" % "grpc-netty" % versions.grpc,
      "io.grpc" % "grpc-protobuf" % versions.grpc,
      "io.grpc" % "grpc-stub" % versions.grpc,
      // For Java
      "io.grpc" % "protoc-gen-grpc-java" % versions.grpc asProtocPlugin,
      "javax.annotation" % "javax.annotation-api" % versions.javaxAnnotation
    ),
    Compile / PB.targets := Seq(
      // For Scala
      scalapb.gen(grpc = true) -> (Compile / sourceManaged).value,
      // For Java
      PB.gens.java -> (Compile / sourceManaged).value,
      PB.gens.plugin("grpc-java") -> (Compile / sourceManaged).value
    )
    // TODO Get proto lite working
//    Compile / PB.protocOptions ++= Seq(
//      "--java_out=lite:" + (Compile / sourceManaged).value
//    )
  )
  .enablePlugins(ProtocPlugin)
