import sbt._

object Dependencies {

  object V {
    val atto           = "0.9.5"
    val catsCore       = "2.8.0"
    val catsEffect     = "3.3.14"
    val ciris          = "2.4.0"
    val flyway         = "9.4.0"
    val scalaTest      = "3.2.14"
    val telegramium    = "7.62.0"
    val testContainers = "0.40.11"
    val mouse          = "1.2.0"
  }

  val common: Seq[ModuleID] =
    Seq(
      "com.dimafeng"          %% "testcontainers-scala-postgresql" % V.testContainers % Test,
      "com.dimafeng"          %% "testcontainers-scala-scalatest"  % V.testContainers % Test,
      "io.github.apimorphism" %% "telegramium-core"                % V.telegramium,
      "io.github.apimorphism" %% "telegramium-high"                % V.telegramium,
      "is.cir"                %% "ciris"                           % V.ciris,
      "org.flywaydb"          %  "flyway-core"                     % V.flyway,
      "org.scalatest"         %% "scalatest"                       % V.scalaTest % Test,
      "org.tpolecat"          %% "atto-core"                       % V.atto,
      "org.tpolecat"          %% "atto-refined"                    % V.atto,
      "org.typelevel"         %% "cats-core"                       % V.catsCore,
      "org.typelevel"         %% "cats-effect"                     % V.catsEffect,
      "org.typelevel"         %% "mouse"                           % V.mouse
  )
}
