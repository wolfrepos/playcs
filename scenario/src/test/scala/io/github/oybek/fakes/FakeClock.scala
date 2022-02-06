package io.github.oybek.fakes

import cats.Applicative
import cats.implicits.catsSyntaxApplicativeId
import io.github.oybek.common.time.Clock

import java.time.Instant

class FakeClock[F[_]: Applicative] extends Clock[F]:
  override def instantNow: F[Instant] =
    Instant.ofEpochMilli(0L).pure[F]
