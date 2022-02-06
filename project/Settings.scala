import sbt.Keys.{libraryDependencies, scalaVersion}
import sbt.{File, Project}
import scoverage.ScoverageKeys.{coverageFailOnMinimum, coverageMinimumStmtTotal}

object Settings {

  def module(name: String, file: File, deps : sbt.ClasspathDep[sbt.ProjectReference]*): Project =
    Project(name, file)
      .settings(scalaVersion := "3.1.1")
      .settings(libraryDependencies ++= Dependencies.common)
      .settings(libraryDependencies ~= { _.map(_.exclude("org.slf4j", "slf4j-simple")) })
      .settings(coverageFailOnMinimum := true)
      .settings(coverageMinimumStmtTotal := 70)
      .dependsOn(deps : _*)
}
