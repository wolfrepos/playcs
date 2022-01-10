package io.github.oybek.fakes

import cats.{Applicative, Id}
import cats.implicits.catsSyntaxApplicativeId
import org.typelevel.log4cats.MessageLogger

class FakeMessageLogger[F[_]: Applicative] extends MessageLogger[F] {
  override def error(message: => String): F[Unit] = ???

  override def warn(message: => String): F[Unit] = ???

  override def info(message: => String): F[Unit] = ().pure[F]

  override def debug(message: => String): F[Unit] = ???

  override def trace(message: => String): F[Unit] = ???
}
