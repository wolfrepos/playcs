package io.github.oybek.common

import cats.effect.{Sync, Temporal}
import cats.implicits.{catsSyntaxApplicativeError, catsSyntaxApplicativeId}
import cats.syntax.flatMap.toFlatMapOps
import cats.syntax.functor.toFunctorOps
import cats.{MonadError, MonadThrow}

import scala.concurrent.duration.FiniteDuration
import scala.util.control.NonFatal

object Scheduler:
  extension [F[_]: Temporal: MonadThrow](action: F[Unit])
    def every(finiteDuration: FiniteDuration): F[Unit] =
      for {
        _ <- action.onError { case NonFatal(_) => ().pure[F] }
        _ <- Temporal[F].sleep(finiteDuration)
        _ <- every(finiteDuration)
      } yield ()
