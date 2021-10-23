package io.github.oybek.fakes

import cats.Applicative
import cats.effect.{Clock, Timer}
import cats.implicits.catsSyntaxApplicativeId

import scala.concurrent.duration.FiniteDuration

class FakeTimer[F[_]: Applicative](theClock: Clock[F]) extends Timer[F] {
  override def clock: Clock[F] = theClock

  override def sleep(duration: FiniteDuration): F[Unit] = ().pure[F]
}
