package io.github.oybek.playcs.service

import cats.Applicative
import cats.implicits.catsSyntaxApplicativeId
import scala.util.Random

trait PasswordService[F[_]]:
  def generate: F[String]

object PasswordService:
  def create[F[_]: Applicative]: PasswordService[F] =
    new PasswordService[F]:
      override def generate: F[String] =
        (Random.nextInt(passwordUpperBorder) + passwordOffset).toString.pure[F]
      private val passwordUpperBorder = 9000
      private val passwordOffset = 1000
