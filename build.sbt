ThisBuild / version := "0.1"
ThisBuild / organization := "io.github.oybek"
ThisBuild / scalaVersion := "2.13.6"

scalacOptions ++= Seq(
 "-encoding", "utf8", // Option and arguments on same line
 "-Xfatal-warnings",  // New lines for each options
 "-deprecation",
 "-unchecked",
 "-language:implicitConversions",
 "-language:higherKinds",
 "-language:existentials",
 "-language:postfixOps"
)

lazy val playcs = (project in file("."))
  .settings(name := "playcs")
  .settings(libraryDependencies ++= Dependencies.common)
  .settings(libraryDependencies ~= { _.map(_.exclude("org.slf4j", "slf4j-simple")) })
  .dependsOn(cstrike, common)

lazy val cstrike = (project in file("cstrike"))
  .settings(name := "cstrike")
  .settings(libraryDependencies ++= Dependencies.common)
  .settings(libraryDependencies ~= { _.map(_.exclude("org.slf4j", "slf4j-simple")) })

lazy val common = (project in file("common"))
  .settings(name := "common")
  .settings(libraryDependencies ++= Dependencies.common)
  .settings(libraryDependencies ~= { _.map(_.exclude("org.slf4j", "slf4j-simple")) })
