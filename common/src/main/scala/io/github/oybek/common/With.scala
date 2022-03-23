package io.github.oybek.common

infix case class With[+T, M](get: T, meta: M)

extension [T](model: T)
  infix def and[M](meta: M): With[T, M] =
    With(model, meta)
