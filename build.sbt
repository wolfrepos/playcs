import Settings.module
import scoverage.ScoverageKeys.coverageExcludedPackages

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

lazy val common =
  module(name = "common", file = file("common"))
    .settings(
      coverageExcludedPackages := Seq(
        "io.github.oybek.common.Scheduler"
      ).mkString(";")
    )

lazy val cstrike = module("cstrike", file("cstrike"))
lazy val scenario = module("scenario", file("scenario"), playcs)
lazy val database = module("database", file("database"))
lazy val playcs =
  module("playcs", file("."), cstrike, common, database)
    .settings(
      coverageExcludedPackages := Seq(
        "Application",
        "integration.HLDSConsoleClient",
        "integration.TGGate",
        "service.impl.HldsConsoleImpl",
        "common.Scheduler",
      ).map(x => s"${(ThisBuild / organization).value}.$x").mkString(";")
    )

// Custom tasks
lazy val testAll = taskKey[Unit]("Run all tests")
testAll := {
  (common / assembly / test).value
  (cstrike / assembly / test).value
  (database / assembly / test).value
  (scenario / assembly / test).value
  (assembly / test).value
}

lazy val checkCoverage = taskKey[Unit]("Check coverage for all subprojects")
checkCoverage := {
  (common / coverageReport).value
  (cstrike / coverageReport).value
  (database / assembly / test).value
  (scenario / coverageReport).value
  coverageReport.value
}
