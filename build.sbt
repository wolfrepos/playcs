import Settings.module

Global / concurrentRestrictions += Tags.limit(Tags.Test, 1)

ThisBuild / version := "0.1"
ThisBuild / organization := "io.github.oybek"
ThisBuild / scalaVersion := "3.1.1"
ThisBuild / parallelExecution := false
ThisBuild / scalacOptions ++= Seq(
 "-encoding", "utf8",
 "-Xfatal-warnings",
 "-deprecation",
 "-unchecked",
 "-language:implicitConversions",
 "-language:higherKinds",
 "-language:existentials",
 "-language:postfixOps",
 "-source:future"
)

lazy val common   = module("common",   file("common"))
lazy val cstrike  = module("cstrike",  file("cstrike"))
lazy val database = module("database", file("database"))
lazy val scenario = module("scenario", file("scenario"), common, playcs)
lazy val playcs   = module("playcs",   file("playcs"), cstrike, common, database)
  .enablePlugins(JacocoCoverallsPlugin)
  .settings(
    jacocoCoverallsServiceName := "github-actions", 
    jacocoCoverallsBranch := sys.env.get("GITHUB_REF_NAME"),
    jacocoCoverallsPullRequest := sys.env.get("GITHUB_EVENT_NAME"),
    jacocoCoverallsRepoToken := sys.env.get("COVERALLS_REPO_TOKEN")
  )
