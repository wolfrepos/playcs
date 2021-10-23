package io.github.oybek.setup

import cats.Id

object TestEffect {
  type F[T] = Id[T]
}
