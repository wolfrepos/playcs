package io.github.oybek.common

import cats.effect.{Sync, Timer}
import cats.syntax.all._

import scala.concurrent.duration.FiniteDuration

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
