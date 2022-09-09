package io.github.oybek.playcs.setup

import cats.Id

object TestEffect:
  type F[T] = Either[Throwable, T]
  type DB[T] = Id[T]
