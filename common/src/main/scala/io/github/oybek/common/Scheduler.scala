package io.github.oybek.common

import cats.MonadError
import cats.effect.{Sync, Temporal}
import cats.implicits.{catsSyntaxApplicativeError, catsSyntaxApplicativeId}
import cats.syntax.flatMap._
import cats.syntax.functor._

import scala.concurrent.duration.FiniteDuration
import scala.language.implicitConversions
import scala.util.control.NonFatal

object Scheduler {
  type ThrowableMonad[F[_]] = MonadError[F, Throwable]

  class ActionOps[F[_]: Temporal: ThrowableMonad](action: F[Unit]) {
    def every(finiteDuration: FiniteDuration): F[Unit] =
      for {
        _ <- action.onError { case NonFatal(_) => ().pure[F] }
        _ <- Temporal[F].sleep(finiteDuration)
        _ <- every(finiteDuration)
      } yield ()
  }

  implicit def toActionOps[F[_]: Temporal: ThrowableMonad](action: F[Unit]): ActionOps[F] =
    new ActionOps[F](action)
}
