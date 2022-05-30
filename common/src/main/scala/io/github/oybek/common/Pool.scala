package io.github.oybek.common

import cats.Monad
import cats.effect.Ref
import cats.implicits.catsSyntaxApplicativeErrorId
import cats.implicits.catsSyntaxApplicativeId
import cats.implicits.catsSyntaxOptionId
import cats.implicits.toTraverseOps
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import io.github.oybek.common.With
import io.github.oybek.common.and
import telegramium.bots.ChatIntId

import scala.concurrent.duration.FiniteDuration
import cats.effect.kernel.Ref.Make

trait Pool[F[_], Id, T]:
  def rent(id: Id): F[Option[T]]
  def find(id: Id): F[Option[T]]
  def free(id: Id): F[Unit]

object Pool:
  def create[F[_]: Monad: Make, Id, T](
      pool: (List[T], List[T With Id]),
      reset: T => F[Unit]
  ): F[Pool[F, Id, T]] =
    Ref.of[F, (List[T], List[T With Id])](pool).map { poolRef =>
      new Pool[F, Id, T]:
        def rent(id: Id): F[Option[T]] =
          poolRef.get.flatMap {
            case (Nil, busy) => Option.empty[T].pure[F]
            case (x :: xs, busy) =>
              poolRef.set((xs, (x and id) :: busy)) >>
                reset(x) >>
                Some(x).pure[F]
          }

        def find(id: Id): F[Option[T]] =
          poolRef.get.map { case (_, busy) =>
            busy.find(_.meta == id).map(_.get)
          }

        override def free(id: Id): F[Unit] =
          for
            (free, busy) <- poolRef.get
            (toFreeWithId, leftBusy) = busy.partition(_.meta == id)
            toFree = toFreeWithId.map(_.get)
            _ <- toFree.traverse(reset)
            _ <- poolRef.set((free ++ toFree, leftBusy))
          yield ()
    }
