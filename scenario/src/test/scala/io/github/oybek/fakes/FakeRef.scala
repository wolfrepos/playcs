package io.github.oybek.fakes

import cats.Applicative
import cats.data.State
import cats.effect.Ref
import cats.implicits.catsSyntaxApplicativeId

class FakeRef[F[_]: Applicative, A](a: A) extends Ref[F, A] {
  override def get: F[A] = value.pure[F]

  override def set(a: A): F[Unit] = (value = a).pure[F]

  override def access: F[(A, A => F[Boolean])] = ???

  override def tryUpdate(f: A => A): F[Boolean] = ???

  override def tryModify[B](f: A => (A, B)): F[Option[B]] = ???

  override def update(f: A => A): F[Unit] = ???

  override def modify[B](f: A => (A, B)): F[B] = ???

  override def tryModifyState[B](state: State[A, B]): F[Option[B]] = ???

  override def modifyState[B](state: State[A, B]): F[B] = ???

  private var value = a
}
