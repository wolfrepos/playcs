package io.github.oybek.common

import io.github.oybek.common.WithMeta.toMetaOps
import org.scalatest.funsuite.AnyFunSuite

class WithMetaSpec extends AnyFunSuite {

  test("PasswordGeneratorImpl.generate should generate 4 digit password") {
    assert(1.withMeta(2) == WithMeta(1, 2))
  }
}
