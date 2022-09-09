package io.github.oybek.playcs.fakes

import cats.Applicative
import cats.implicits.catsSyntaxApplicativeId
import io.github.oybek.playcs.fakes.FakeData.fakePassword
import io.github.oybek.playcs.service.PasswordService

class FakePasswordGenerator[F[_]: Applicative] extends PasswordService[F]:
  override def generate: F[String] = fakePassword.pure[F]
