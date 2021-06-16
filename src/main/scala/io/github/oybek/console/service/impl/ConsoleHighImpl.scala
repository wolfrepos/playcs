package io.github.oybek.console.service.impl

import cats.effect.{Concurrent, Timer}
import io.github.oybek.console.service.{ConsoleHigh, ConsoleLow}

class ConsoleHighImpl[F[_]: Timer: Concurrent](val ip: String,
                                               val port: Int,
                                               private val consoleLow: ConsoleLow[F]) extends ConsoleHigh[F] {

  override def map(map: String): F[Unit] =
    consoleLow.execute(s"map $map")

  override def changeLevel(map: String): F[Unit] =
    consoleLow.execute(s"changelevel $map")

  override def hostname(name: String): F[Unit] =
    consoleLow.execute(s"hostname $name")

  override def svPassword(password: String): F[Unit] =
    consoleLow.execute(s"sv_password $password")
}
