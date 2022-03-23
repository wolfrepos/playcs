import Settings.module

ThisBuild / version := "0.1"
ThisBuild / organization := "io.github.oybek"
ThisBuild / scalaVersion := "3.1.1"
ThisBuild / scalacOptions ++= Seq(
 "-encoding", "utf8", // Option and arguments on same line
 "-Xfatal-warnings",  // New lines for each options
 "-deprecation",
 "-unchecked",
 "-language:implicitConversions",
 "-language:higherKinds",
 "-language:existentials",
 "-language:postfixOps",
 "-source:future"
)

lazy val common = module(name = "common", file = file("common"))
lazy val cstrike = module("cstrike", file("cstrike"))
lazy val scenario = module("scenario", file("scenario"), common, playcs)
lazy val database = module("database", file("database"))
lazy val playcs = module("playcs", file("playcs"), cstrike, common, database)

// Custom tasks
lazy val testAll = taskKey[Unit]("Run all tests")
testAll := {
  (common / Test / test).value
  (cstrike / Test / test).value
  (database / Test / test).value
  (scenario / Test / test).value
  (playcs / Test / test).value
}
