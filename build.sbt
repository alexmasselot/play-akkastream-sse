name := """play-akkastream"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  ws,
  "com.typesafe.akka" %% "akka-stream" % "2.4.12"
)

