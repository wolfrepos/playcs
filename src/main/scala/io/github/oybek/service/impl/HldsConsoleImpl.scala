package io.github.oybek.service.impl

import io.github.oybek.integration.HLDSConsoleClient
import io.github.oybek.service.HldsConsole

class HldsConsoleImpl[F[_]](val ip: String,
                            val port: Int,
                            val consoleLow: HLDSConsoleClient[F]) extends HldsConsole[F]:

  override def map(map: String): F[Unit] =
    consoleLow.execute(s"map $map")

  override def changeLevel(map: String): F[Unit] =
    consoleLow.execute(s"changelevel $map")

  override def hostname(name: String): F[Unit] =
    consoleLow.execute(s"hostname $name")

  override def svPassword(password: String): F[Unit] =
    consoleLow.execute(s"sv_password $password")

  override def say(text: String): F[Unit] =
    consoleLow.execute(s"say $text")
