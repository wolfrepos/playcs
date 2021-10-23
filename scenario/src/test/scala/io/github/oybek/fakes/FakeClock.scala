package io.github.oybek.fakes

import cats.Applicative
import cats.effect.Clock
import cats.implicits.catsSyntaxApplicativeId

import scala.concurrent.duration.TimeUnit

class FakeClock[F[_]: Applicative] extends Clock[F] {
  override def realTime(unit: TimeUnit): F[Long] =
    time.pure[F]

  override def monotonic(unit: TimeUnit): F[Long] = ???

  def setTime(t: Long): Unit =
    time = t

  private var time = 0L
}
