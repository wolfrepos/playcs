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

addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")

lazy val playcs = (project in file("."))
  .settings(name := "playcs")
  .settings(libraryDependencies ++= Dependencies.common)
  .dependsOn(cstrike, common)

lazy val cstrike = (project in file("cstrike"))
  .settings(name := "cstrike")
  .settings(libraryDependencies ++= Dependencies.common)

lazy val common = (project in file("common"))
  .settings(name := "common")
  .settings(libraryDependencies ++= Dependencies.common)

assemblyMergeStrategy in assembly := {
 case PathList("META-INF", _ @ _*) => MergeStrategy.discard
 case _ => MergeStrategy.first
}
