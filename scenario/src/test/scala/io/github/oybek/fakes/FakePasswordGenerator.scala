package io.github.oybek.fakes

import cats.Applicative
import cats.implicits.catsSyntaxApplicativeId
import io.github.oybek.fakes.FakeData.fakePassword
import io.github.oybek.service.PasswordGenerator

class FakePasswordGenerator[F[_]: Applicative] extends PasswordGenerator[F] {
  override def generate: F[String] = fakePassword.pure[F]
}
