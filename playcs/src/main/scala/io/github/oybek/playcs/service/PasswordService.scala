package io.github.oybek.playcs.service

import cats.Applicative
import cats.effect.IO
import cats.implicits.catsSyntaxApplicativeId
import scala.util.Random

trait PasswordService:
  def generate: IO[String]

object PasswordService:
  def create: PasswordService =
    new PasswordService:
      override def generate: IO[String] =
        (Random.nextInt(passwordUpperBorder) + passwordOffset).toString.pure[IO]
      private val passwordUpperBorder = 9000
      private val passwordOffset = 1000
