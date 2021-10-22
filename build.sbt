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
