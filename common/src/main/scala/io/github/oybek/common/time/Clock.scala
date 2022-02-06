package io.github.oybek.common.time

import java.time.Instant

trait Clock[F[_]]:
  def instantNow: F[Instant]

object Clock:
  def apply[F[_]](using clock: Clock[F]): Clock[F] = clock
