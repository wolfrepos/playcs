package io.github.oybek.common

infix case class WithMeta[+T, M](get: T, meta: M)

extension [T](model: T)
  infix def withMeta[M](meta: M): WithMeta[T, M] =
    WithMeta(model, meta)
