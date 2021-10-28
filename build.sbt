import Settings.module

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

lazy val common  = module("common", file("common"))
lazy val cstrike = module("cstrike", file("cstrike"))
lazy val scenario = module("scenario", file("scenario"), playcs)
lazy val playcs  = module("playcs", file("."), cstrike, common)

// Custom tasks
lazy val testAll = taskKey[Unit]("Run all tests")
testAll := {
  (common / assembly / test).value
  (cstrike / assembly / test).value
  (scenario / assembly / test).value
  (assembly / test).value
}

lazy val checkCoverage = taskKey[Unit]("Check coverage for all subprojects")
checkCoverage := {
  (common / coverageReport).value
  (cstrike / coverageReport).value
  (scenario / coverageReport).value
  coverageReport.value
}
