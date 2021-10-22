package io.github.oybek

import cats.effect.IO
import io.github.oybek.service.HldsConsole
import org.scalamock.scalatest.MockFactory
import org.scalatest.funsuite.AnyFunSuite
import telegramium.bots.high.Api

class HelpCommandScenario extends AnyFunSuite with MockFactory {

  test("An empty Set should have size 0") {
    assert(Set.empty.size == 0)
  }

  test("Invoking head on an empty Set should produce NoSuchElementException") {
    assertThrows[NoSuchElementException] {
      Set.empty.head
    }
  }

  val console: HldsConsole[IO] = mock[HldsConsole[IO]]
  val apiMock: Api[IO] = mock[Api[IO]]
}
