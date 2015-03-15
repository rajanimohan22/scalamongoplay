name := """scalaactivatormongo"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  javaJdbc,
  javaEbean
)

libraryDependencies ++= Seq(
  cache,
  "org.mongodb" %% "casbah" % "2.8.0"
)

