package io.github.oybek.service.console

trait ConsoleAlg[F[_]] {

  def println(s: String): F[Unit]
  def readln: F[Option[String]]
}
