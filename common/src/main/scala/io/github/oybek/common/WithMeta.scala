package io.github.oybek.common

import cats.Functor
import cats.implicits._

case class WithMeta[+T, M](get: T, meta: M)

object WithMeta {
  class MetaOps[T](val model: T) extends AnyVal {
    def withMeta[M](meta: M): WithMeta[T, M] =
      WithMeta(model, meta)

    def withMetaF[F[_]: Functor, M](metaF: F[M]): F[WithMeta[T, M]] =
      metaF.map(WithMeta(model, _))
  }

  class MetaOps2[T, M](val withMeta: T WithMeta M) extends AnyVal {
    def updateMeta(meta: M): WithMeta[T, M] =
      WithMeta(withMeta.get, meta)
  }

  implicit def toMetaOps[T](model: T): MetaOps[T] =
    new MetaOps(model)

  implicit def toMetaOps2[T, M](withMeta: T WithMeta M): MetaOps2[T, M] =
    new MetaOps2(withMeta)
}
