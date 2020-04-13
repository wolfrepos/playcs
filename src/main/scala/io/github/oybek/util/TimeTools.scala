package io.github.oybek.util

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

import cats.effect.{Sync, Timer}
import cats.syntax.all._

import scala.concurrent.duration.{FiniteDuration, _}

object TimeTools {
  implicit class PF[F[_]: Sync: Timer](ff: F[Unit]) {
    def every(finiteDuration: FiniteDuration): F[Unit] =
      for {
        _ <- ff
        _ <- Timer[F].sleep(finiteDuration)
        _ <- every(finiteDuration)
      } yield ()
  }
}
