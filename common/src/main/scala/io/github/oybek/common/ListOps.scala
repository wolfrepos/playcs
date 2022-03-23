package io.github.oybek.common

import cats.Monad
import cats.implicits.toFlatMapOps
import cats.implicits.catsSyntaxApplicativeId

object ListOps:
  extension[F[_]: Monad, T](list: List[F[Option[T]]]) {
    def firstSomeF: F[Option[T]] =
      _firstSomeF(list)

    private def _firstSomeF(l: List[F[Option[T]]]): F[Option[T]] =
      l match {
        case Nil => Option.empty[T].pure[F]
        case x::xs =>
          x.flatMap {
            case None => _firstSomeF(xs)
            case Some(v) => Some(v).pure[F]
          }
      }
  }

