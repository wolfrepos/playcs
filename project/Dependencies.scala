import sbt._

object Dependencies {

  object V {
    val atto = "0.7.0"
    val catsCore = "2.6.1"
    val catsEffect = "2.0.0"
    val scalaTest = "3.1.1"
    val scalaMock = "5.1.0"
    val pureConfig = "0.12.3"
  }

  val catsCore   = "org.typelevel" %% "cats-core" % V.catsCore
  val catsEffect = "org.typelevel" %% "cats-effect" % V.catsEffect
  val pureConfig = "com.github.pureconfig" %% "pureconfig" % V.pureConfig
  val scalaMock  = "org.scalamock" %% "scalamock" % V.scalaMock % Test
  val scalaTest  = "org.scalatest" %% "scalatest" % V.scalaTest % Test

  val http4s = Seq(
    "org.http4s" %% "http4s-blaze-client" % "0.21.18"
  )

  val atto = Seq(
    "org.tpolecat" %% "atto-core"    % V.atto,
    "org.tpolecat" %% "atto-refined" % V.atto
  )

  val telegramium = Seq(
    "io.github.apimorphism" %% "telegramium-core" % "3.50.0",
    "io.github.apimorphism" %% "telegramium-high" % "3.50.0"
  )

  val common = Seq(catsCore, catsEffect, scalaTest, scalaMock, pureConfig) ++ atto ++ http4s ++ telegramium
}
