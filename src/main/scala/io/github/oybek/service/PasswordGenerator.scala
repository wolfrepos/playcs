package io.github.oybek.service

trait PasswordGenerator[F[_]] {
  def generate: F[String]
}
