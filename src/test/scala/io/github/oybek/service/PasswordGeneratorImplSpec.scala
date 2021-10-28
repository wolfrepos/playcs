package io.github.oybek.service

import cats.Id
import io.github.oybek.service.impl.PasswordGeneratorImpl
import org.scalatest.funsuite.AnyFunSuite

class PasswordGeneratorImplSpec extends AnyFunSuite {

  test("PasswordGeneratorImpl.generate should generate 4 digit password") {
    val password = passwordGenerator.generate
    assert(password.forall(_.isDigit))
    assert(password.length == 4)
  }

  private lazy val passwordGenerator = new PasswordGeneratorImpl[Id]
}
