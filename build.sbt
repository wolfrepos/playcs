ThisBuild / version := "0.1"
ThisBuild / organization := "io.github.oybek"

val settings = Compiler.settings ++ Seq()

lazy val playcs = (project in file("."))
  .settings(name := "playcs")
  .settings(libraryDependencies ++= Dependencies.common)
  .settings(sonarProperties := Sonar.properties)
  .settings(Compiler.settings)

assemblyMergeStrategy in assembly := {
 case PathList("META-INF", _ @ _*) => MergeStrategy.discard
 case _ => MergeStrategy.first
}
