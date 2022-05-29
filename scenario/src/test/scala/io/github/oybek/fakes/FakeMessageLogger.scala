package io.github.oybek.fakes

import cats.{Applicative, Id}
import cats.implicits.catsSyntaxApplicativeId
import io.github.oybek.common.logger.Context
import org.typelevel.log4cats.Logger

class FakeMessageLogger[F[_]: Applicative] extends Logger[F]:
  override def error(message: => String): F[Unit] = ().pure[F]
  override def warn(message: => String): F[Unit]  = ().pure[F]
  override def info(message: => String): F[Unit]  = ().pure[F]
  override def debug(message: => String): F[Unit] = ().pure[F]
  override def trace(message: => String): F[Unit] = ().pure[F]

  override def error(t: Throwable)(message: => String): F[Unit] = ().pure[F]
  override def warn(t: Throwable)(message: => String): F[Unit]  = ().pure[F]
  override def info(t: Throwable)(message: => String): F[Unit]  = ().pure[F]
  override def debug(t: Throwable)(message: => String): F[Unit] = ().pure[F]
  override def trace(t: Throwable)(message: => String): F[Unit] = ().pure[F]
