package io.github.oybek

import cats.implicits.catsSyntaxOptionId
import io.github.oybek.cstrike.model.Command.helpText
import io.github.oybek.fakes.FakeData.fakeChatId
import io.github.oybek.model.Reaction.SendText
import io.github.oybek.service.Console
import io.github.oybek.setup.ConsoleSetup
import io.github.oybek.setup.TestEffect.F
import org.scalatest.GivenWhenThen
import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import telegramium.bots.Markdown

class JoinCommandScenario extends AnyFeatureSpec with GivenWhenThen with ConsoleSetup {

  info("As a user")
  info("I want to be able to join dedicated counter strike server")

  override val console: Console[F] = setupConsole

  Feature("/join command") {
    Scenario("User gives command '/join' before '/new' command") {
      Given("console without created server")
      When("/join command received")
      Then("message about server creation is returned")
      console.handle(fakeChatId, "/join") shouldEqual
        Right(List(SendText(fakeChatId, "Создай сервер сначала (/help)")))
    }

    Scenario("User gives command '/join' after '/new' command") {
      Given("console with created server")
      console.handle(fakeChatId, "/new")
      When("/join command received")
      Then("command to join is returned")
      console.handle(fakeChatId, "/join") shouldEqual
        Right(List(SendText(fakeChatId, "`connect 127.0.0.1:27015; password 4444`", Markdown.some)))
    }

    Scenario("User gives command '/join' after '/new' and '/free' commands") {
      Given("console with created and then released server")
      console.handle(fakeChatId, "/new")
      console.handle(fakeChatId, "/free")
      When("/join command received")
      Then("message about server creation is returned")
      console.handle(fakeChatId, "/join") shouldEqual
        Right(List(SendText(fakeChatId, "Создай сервер сначала (/help)")))
    }

    Scenario("User gives command '/join' after '/new' and '/free' and '/new' commands") {
      Given("console with created and then released and then created server")
      console.handle(fakeChatId, "/new")
      console.handle(fakeChatId, "/free")
      console.handle(fakeChatId, "/new")
      When("/join command received")
      Then("command to join is returned")
      console.handle(fakeChatId, "/join") shouldEqual
        Right(List(SendText(fakeChatId, "`connect 127.0.0.1:27015; password 4444`", Markdown.some)))
    }

  }
}
