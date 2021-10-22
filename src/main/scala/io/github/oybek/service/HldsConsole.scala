package io.github.oybek.service

// Half Life Dedicated Server Console

trait HldsConsole[F[_]] {
  def map(map: String): F[Unit]
  def svPassword(password: String): F[Unit]
  def hostname(name: String): F[Unit]
  def changeLevel(map: String): F[Unit]
  def ip: String
  def port: Int
}
