package io.github.oybek.fakes

import cats.Applicative
import cats.implicits.catsSyntaxApplicativeId
import io.github.oybek.common.time.Timer

import scala.concurrent.duration.FiniteDuration

class FakeTimer[F[_]: Applicative] extends Timer[F] {
  override def sleep(duration: FiniteDuration): F[Unit] = ().pure[F]
}
