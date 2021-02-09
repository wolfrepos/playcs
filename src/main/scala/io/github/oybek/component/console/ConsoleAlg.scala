package io.github.oybek.component.console

trait ConsoleAlg[F[_]] {

  def execute(s: String): F[Unit]
  def readln: F[Option[String]]
}
