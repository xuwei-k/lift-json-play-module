name := "play-argonaut"

organization := "com.github.xuwei-k"

version := "0.1-SNAPSHOT"

scalaVersion := "2.10.0"

scalacOptions ++= Seq("-feature")

resolvers ++= Seq(
  "typesafe" at "http://repo.typesafe.com/typesafe/releases",
  Opts.resolver.sonatypeSnapshots
)

libraryDependencies ++= Seq(
  "io.argonaut" %% "argonaut" % "6.0-SNAPSHOT" cross CrossVersion.full,
  "play" %% "play" % "2.1.0" % "provided",
  "play" %% "play-test" % "2.1.0" % "test"
)
