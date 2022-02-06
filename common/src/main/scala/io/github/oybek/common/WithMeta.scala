package io.github.oybek.common

case class WithMeta[+T, M](get: T, meta: M)

extension [T](model: T)
  def withMeta[M](meta: M): WithMeta[T, M] =
    WithMeta(model, meta)
