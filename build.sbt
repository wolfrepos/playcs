import Settings.module

ThisBuild / version := "0.1"
ThisBuild / organization := "io.github.oybek"
ThisBuild / scalaVersion := "3.2.0"
ThisBuild / scalacOptions ++= Seq(
  "-encoding",
  "utf8",
  "-feature",
  "-Xfatal-warnings",
  "-deprecation",
  "-unchecked",
  "-language:implicitConversions",
  "-language:higherKinds",
  "-language:existentials",
  "-language:postfixOps",
  "-source:future",
  "-language:adhocExtensions"
)

lazy val common = module("common", file("common"))
lazy val cstrike = module("cstrike", file("cstrike"))
lazy val playcs = module("playcs", file("playcs"), cstrike, common)
  .settings(assembly / assemblyJarName := "app.jar")
lazy val scenario = module("scenario", file("scenario"), common, playcs)

Global / concurrentRestrictions += Tags.limit(Tags.Test, 1)

lazy val testAll = taskKey[Unit]("Run all tests")
testAll := {
  (common / Test / test).value
  (cstrike / Test / test).value
  (playcs / Test / test).value
  (scenario / Test / test).value
}
