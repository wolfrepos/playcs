package io.github.oybek.common.logger

import cats.effect.IO
import cats.syntax.functor.*
import cats.effect.kernel.Sync
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

class ContextLogger[F[_]](logger: Logger[F]):
	private def context(message: => String): Context[String] = s"${summon[ContextData].flowId} - $message"
	def error(message: => String): Context[F[Unit]] = logger.error(context(message))
	def warn(message: => String): Context[F[Unit]] = logger.warn(context(message))
	def info(message: => String): Context[F[Unit]] = logger.info(context(message))
	def debug(message: => String): Context[F[Unit]] = logger.debug(context(message))
	def trace(message: => String): Context[F[Unit]] = logger.trace(context(message))
	def error(t: Throwable)(message: => String): Context[F[Unit]] = logger.error(t)(context(message))
	def warn(t: Throwable)(message: => String): Context[F[Unit]] = logger.warn(t)(context(message))
	def info(t: Throwable)(message: => String): Context[F[Unit]] = logger.info(t)(context(message))
	def debug(t: Throwable)(message: => String): Context[F[Unit]] = logger.debug(t)(context(message))
	def trace(t: Throwable)(message: => String): Context[F[Unit]] = logger.trace(t)(context(message))

object ContextLogger:
	def create[F[_]: Sync](name: String): F[ContextLogger[F]] =
		Slf4jLogger.fromName[F](name).map(new ContextLogger(_))
