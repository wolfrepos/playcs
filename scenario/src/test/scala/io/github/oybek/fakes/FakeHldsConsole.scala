package io.github.oybek.fakes

import cats.Applicative
import cats.implicits.{catsSyntaxApplicativeId, toFunctorOps}
import io.github.oybek.fakes.FakeData.{fakeIp, fakePort}
import io.github.oybek.service.HldsConsole

import scala.collection.mutable.ListBuffer

class FakeHldsConsole[F[_]: Applicative] extends HldsConsole[F]:
  override def map(map: String): F[Unit] =
    calledCommands.addOne(s"map $map").pure[F].void

  override def svPassword(password: String): F[Unit] =
    calledCommands.addOne(s"sv_password $password").pure[F].void

  override def hostname(name: String): F[Unit] =
    calledCommands.addOne(s"hostname $name").pure[F].void

  override def changeLevel(map: String): F[Unit] =
    calledCommands.addOne(s"changelevel $map").pure[F].void

  override def say(text: String): F[Unit] =
    calledCommands.addOne(s"say $text").pure[F].void

  override def ip: String = fakeIp
  override def port: Int = fakePort

  def getCalledCommands: List[String] =
    calledCommands.toList

  def reset: Unit = calledCommands.remove(0, calledCommands.size)

  private val calledCommands = ListBuffer.empty[String]
