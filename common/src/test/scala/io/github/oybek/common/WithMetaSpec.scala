package io.github.oybek.common

import io.github.oybek.common.With
import io.github.oybek.common.and
import org.scalatest.funsuite.AnyFunSuite

class WithSpec extends AnyFunSuite:
  test("PasswordGeneratorImpl.generate should generate 4 digit password") {
    assert(1.and(2) == With(1, 2))
  }
