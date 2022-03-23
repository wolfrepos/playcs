package io.github.oybek.service

import cats.Applicative
import cats.implicits.catsSyntaxApplicativeId
import io.github.oybek.service.PasswordGenerator
import scala.util.Random

trait PasswordGenerator[F[_]]:
  def generate: F[String]

object PasswordGenerator:
  def create[F[_]: Applicative]: PasswordGenerator[F] =
    new PasswordGenerator[F]:
      override def generate: F[String] =
        (Random.nextInt(passwordUpperBorder) + passwordOffset).toString.pure[F]
      private val passwordUpperBorder = 9000
      private val passwordOffset = 1000
