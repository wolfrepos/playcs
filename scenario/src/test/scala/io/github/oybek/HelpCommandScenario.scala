package io.github.oybek

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import io.github.oybek.common.logger.ContextData
import io.github.oybek.cstrike.model.Command.helpText
import io.github.oybek.fakes.FakeData.fakeChatId
import io.github.oybek.fakes.FakeData.fakeUser
import io.github.oybek.model.Reaction.SendText
import io.github.oybek.setup.HubSetup
import org.scalatest.GivenWhenThen
import org.scalatest.funsuite.AnyFunSuite

trait HelpCommandScenario extends AnyFunSuite with HubSetup:
  test("/help command scenario") {
    (for
      (hub, _, _, _) <- createHub(hldsNum = 1)
      reaction <- hub.handle(fakeChatId, fakeUser, "/help")
      _ = assert(reaction === List(SendText(fakeChatId, helpText)))
    yield()).unsafeRunSync()
  }