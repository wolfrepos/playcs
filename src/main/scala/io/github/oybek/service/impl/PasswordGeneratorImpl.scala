package io.github.oybek.service.impl

import cats.Applicative
import cats.implicits.catsSyntaxApplicativeId
import io.github.oybek.service.PasswordGenerator

import scala.util.Random

class PasswordGeneratorImpl[F[_]: Applicative] extends PasswordGenerator[F] {
  def generate: F[String] =
    (Random.nextInt(passwordUpperBorder) + passwordOffset).toString.pure[F]
  private val passwordUpperBorder = 9000
  private val passwordOffset = 1000
}
