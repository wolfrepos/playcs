package io.github.oybek.playcs.client

trait HldsClient[F[_]]:
  def map(map: String): F[Unit]
  def svPassword(password: String): F[Unit]
  def hostname(name: String): F[Unit]
  def changeLevel(map: String): F[Unit]
  def say(text: String): F[Unit]
  def ip: String
  def port: Int

object HldsClient:
  def create[F[_]](
      anIp: String,
      aPort: Int,
      consoleLow: HldsClientLow[F]
  ): HldsClient[F] = new HldsClient[F]:
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

    override val ip = anIp
    override val port = aPort
