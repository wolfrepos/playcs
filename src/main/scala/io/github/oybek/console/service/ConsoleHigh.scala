package io.github.oybek.console.service

import cats.syntax.functor._
import cats.effect.{Concurrent, Sync, Timer}
import io.github.oybek.console.service.impl.ConsoleHighImpl

import java.io.File

trait ConsoleHigh[F[_]] {
  def map(map: String): F[Unit]
  def svPassword(password: String): F[Unit]
  def hostname(name: String): F[Unit]
  def changeLevel(map: String): F[Unit]

  def ip: String
  def port: Int
}

object ConsoleHigh {
  def create[F[_]: Sync: Timer: Concurrent](ip: String, port: Int, hldsDir: File): F[ConsoleHighImpl[F]] =
    ConsoleLow.create(port, hldsDir).map {
      consoleLow => new ConsoleHighImpl[F](ip, port, consoleLow)
    }
}
