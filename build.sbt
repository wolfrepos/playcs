ThisBuild / version := "0.1"
ThisBuild / organization := "io.github.oybek"

val settings = Compiler.settings ++ Seq()

lazy val playcs = (project in file("."))
  .settings(name := "playcs")
  .settings(libraryDependencies ++= Dependencies.common)
  .settings(Compiler.settings)
  .dependsOn(cstrike, common)

lazy val cstrike = (project in file("cstrike"))
  .settings(name := "cstrike")
  .settings(libraryDependencies ++= Dependencies.common)
  .settings(Compiler.settings)

lazy val common = (project in file("common"))
  .settings(name := "common")
  .settings(libraryDependencies ++= Dependencies.common)
  .settings(Compiler.settings)

assemblyMergeStrategy in assembly := {
 case PathList("META-INF", _ @ _*) => MergeStrategy.discard
 case _ => MergeStrategy.first
}
