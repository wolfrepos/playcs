package io.github.oybek.playcs.common

import cats.Monad
import cats.effect.Ref
import cats.implicits.catsSyntaxApplicativeErrorId
import cats.implicits.catsSyntaxApplicativeId
import cats.implicits.catsSyntaxOptionId
import cats.implicits.toTraverseOps
import cats.syntax.flatMap.*
import cats.syntax.functor.*

import scala.concurrent.duration.FiniteDuration
import cats.effect.kernel.Ref.Make

trait Pool[F[_], T]:
  def rent(id: Long): F[Option[T]]
  def find(id: Long): F[Option[T]]
  def free(id: Long): F[Unit]

object Pool:
  def create[F[_]: Monad: Make, T](
      pool: (List[T], List[(Long, T)]),
      reset: T => F[Unit]
  ): F[Pool[F, T]] =
    Ref.of[F, (List[T], List[(Long, T)])](pool).map { poolRef =>
      new Pool[F, T]:
        def rent(id: Long): F[Option[T]] =
          poolRef.get.flatMap {
            case (Nil, busy) => None.pure[F]
            case (x :: xs, busy) =>
              poolRef.set((xs, (id, x) :: busy)) >>
                x.some.pure[F]
          }

        def find(id: Long): F[Option[T]] =
          poolRef.get.map((_, busy) =>
            busy.collectFirst {
              case (i, r) if i == id =>
                r
            }
          )

        override def free(id: Long): F[Unit] =
          for
            (free, busy) <- poolRef.get
            (idAndR, leftBusy) = busy.partition((i, _) => i == id)
            (_, toFree) = idAndR.unzip
            _ <- toFree.traverse(reset)
            _ <- poolRef.set((free ++ toFree, leftBusy))
          yield ()
    }
