package io.github.oybek.setup

import cats.Id

object TestEffect {
  type F[T] = Either[Throwable, T]
  type DB[T] = Id[T]
}
