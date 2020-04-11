import sbt._

object Dependencies {

  object V {
    val catsCore = "2.0.0"
    val catsEffect = "2.0.0"
  }

  val catsCore = "org.typelevel" %% "cats-core" % V.catsCore
  val catsEffect = "org.typelevel" %% "cats-effect" % V.catsEffect

  val common = Seq(catsCore, catsEffect)
}
