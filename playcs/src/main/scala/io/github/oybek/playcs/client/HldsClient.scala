package io.github.oybek.playcs.client

import cats.effect.IO

trait HldsClient:
  def map(map: String): IO[Unit]
  def svPassword(password: String): IO[Unit]
  def hostname(name: String): IO[Unit]
  def changeLevel(map: String): IO[Unit]
  def say(text: String): IO[Unit]
  def ip: String
  def port: Int

object HldsClient:
  def create(
      anIp: String,
      aPort: Int,
      hldsDrvier: HldsDriver
  ): HldsClient = new HldsClient:
    override def map(map: String): IO[Unit] =
      hldsDrvier.execute(s"map $map")

    override def changeLevel(map: String): IO[Unit] =
      hldsDrvier.execute(s"changelevel $map")

    override def hostname(name: String): IO[Unit] =
      hldsDrvier.execute(s"hostname $name")

    override def svPassword(password: String): IO[Unit] =
      hldsDrvier.execute(s"sv_password $password")

    override def say(text: String): IO[Unit] =
      hldsDrvier.execute(s"say $text")

    override val ip = anIp
    override val port = aPort
