package io.github.oybek.common

case class WithMeta[+T, M](get: T, meta: M)

object WithMeta {
  class MetaOps[T](val model: T) extends AnyVal {
    def withMeta[M](meta: M): WithMeta[T, M] =
      WithMeta(model, meta)
  }

  implicit def toMetaOps[T](model: T): MetaOps[T] =
    new MetaOps(model)
}
