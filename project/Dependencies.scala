import sbt.*

object Dependencies {

  object V {
    val atto = "0.9.5"
    val catsCore = "2.7.0"
    val catsEffect = "3.3.3"
    val scalaTest = "3.2.11"
    val doobie = "1.0.0-RC1"
    val testContainers = "0.39.12"
    val flyway = "8.4.0"
    val ciris = "2.3.1"
  }

  val catsCore   = "org.typelevel" %% "cats-core" % V.catsCore
  val catsEffect = "org.typelevel" %% "cats-effect" % V.catsEffect
  val scalaTest  = "org.scalatest" %% "scalatest" % V.scalaTest % Test
  val flyway     = "org.flywaydb" % "flyway-core" % V.flyway
  val ciris      = "is.cir" %% "ciris" % V.ciris

  val doobie     = Seq(
    "org.tpolecat" %% "doobie-core" % V.doobie,
    "org.tpolecat" %% "doobie-hikari" % V.doobie,
    "org.tpolecat" %% "doobie-postgres" % V.doobie
  )

  val testContainers = Seq(
    "com.dimafeng" %% "testcontainers-scala-scalatest" % V.testContainers % Test,
    "com.dimafeng" %% "testcontainers-scala-postgresql" % V.testContainers % Test
  )

  val atto = Seq(
    "org.tpolecat" %% "atto-core"    % V.atto,
    "org.tpolecat" %% "atto-refined" % V.atto
  )

  val telegramium = Seq(
    "io.github.apimorphism" %% "telegramium-core" % "7.56.0",
    "io.github.apimorphism" %% "telegramium-high" % "7.56.0"
  )

  val common: Seq[ModuleID] =
    Seq(
      catsCore,
      catsEffect,
      ciris,
      flyway,
      scalaTest,
    ) ++
    atto ++
    telegramium ++
    testContainers ++
    doobie
}
