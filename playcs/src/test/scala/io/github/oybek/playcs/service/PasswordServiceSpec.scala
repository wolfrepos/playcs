package io.github.oybek.playcs.service

import cats.Id
import io.github.oybek.playcs.service.PasswordService
import org.scalatest.funsuite.AnyFunSuite

class PasswordServiceSpec extends AnyFunSuite:

  test("PasswordGeneratorImpl.generate should generate 4 digit password") {
    val password = passwordGenerator.generate
    assert(password.forall(_.isDigit))
    assert(password.length == 4)
  }

  private lazy val passwordGenerator = PasswordService.create[Id]
