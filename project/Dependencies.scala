import sbt.*

object Dependencies {

  object V {
    val atto           = "0.9.5"
    val catsCore       = "2.7.0"
    val catsEffect     = "3.3.11"
    val ciris          = "2.3.2"
    val doobie         = "1.0.0-RC2"
    val flyway         = "8.5.9"
    val scalaTest      = "3.2.12"
    val telegramium    = "7.60.0"
    val testContainers = "0.40.6"
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
      "org.tpolecat"          %% "doobie-core"                     % V.doobie,
      "org.tpolecat"          %% "doobie-hikari"                   % V.doobie,
      "org.tpolecat"          %% "doobie-postgres"                 % V.doobie,
      "org.tpolecat"          %% "doobie-scalatest"                % V.doobie,
      "org.typelevel"         %% "cats-core"                       % V.catsCore,
      "org.typelevel"         %% "cats-effect"                     % V.catsEffect
    )
}
