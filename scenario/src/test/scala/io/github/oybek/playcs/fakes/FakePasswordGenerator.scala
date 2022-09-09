package io.github.oybek.playcs.fakes

import cats.Applicative
import cats.implicits.catsSyntaxApplicativeId
import io.github.oybek.playcs.fakes.FakeData.fakePassword
import io.github.oybek.playcs.password.PasswordGenerator

class FakePasswordGenerator[F[_]: Applicative] extends PasswordGenerator[F]:
  override def generate: F[String] = fakePassword.pure[F]
