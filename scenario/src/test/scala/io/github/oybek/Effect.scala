package io.github.oybek

import cats.effect.IO

object Effect {
  type F[T] = IO[T]
}
