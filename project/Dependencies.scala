import sbt._

object Dependencies {

  object V {
    val catsCore = "2.0.0"
    val catsEffect = "2.0.0"
    val scalaTest = "3.1.1"
    val telegramium = "1.0.0-RC1"
    val slf4j = "1.7.26"
    val logback = "1.2.3"
    val pureConfig = "0.10.2"
  }

  val catsCore = "org.typelevel" %% "cats-core" % V.catsCore
  val catsEffect = "org.typelevel" %% "cats-effect" % V.catsEffect
  val scalaTest = "org.scalatest" %% "scalatest" % V.scalaTest % "test"
  val pureConfig = "com.github.pureconfig" %% "pureconfig" % V.pureConfig

  val telegramium = Seq(
    "io.github.apimorphism" %% "telegramium-core" % V.telegramium,
    "io.github.apimorphism" %% "telegramium-high" % V.telegramium
  )

  val logger = Seq(
    "org.slf4j" % "slf4j-api" % V.slf4j,
    "ch.qos.logback" % "logback-classic" % V.logback
  )

  val common =
    Seq(
      catsCore,
      catsEffect,
      scalaTest,
      pureConfig
    ) ++
      telegramium ++
      logger
}
