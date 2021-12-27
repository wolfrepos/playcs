package io.github.oybek

import io.github.oybek.common.WithMeta.toMetaOps
import io.github.oybek.fakes.FakeData.{fakeChatId, fakePassword}
import io.github.oybek.model.Reaction.{SendText, Sleep}
import io.github.oybek.model.{ConsoleMeta, ConsolePool}
import io.github.oybek.setup.ConsoleSetup
import org.scalatest.GivenWhenThen
import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import telegramium.bots.Markdown

import java.time.Instant
import scala.concurrent.duration.DurationInt

class NewCommandSpec extends AnyFeatureSpec with GivenWhenThen with ConsoleSetup {

  info("As a user")
  info("I want to be able to create dedicated counter strike server")

  Feature("/new command") {
    Scenario("User gives command '/new'") {
      Given("console which has a free dedicated servers")

      When("/new command received")
      val result = console.handle(fakeChatId, "/new")

      Then("new dedicated server should be created")
      hldsConsole.getCalledCommands shouldEqual List(
        s"sv_password $fakePassword",
        "map de_dust2",
        "changelevel de_dust2"
      )
      consolePoolRef.get shouldEqual
        ConsolePool(
          Nil,
          List(hldsConsole withMeta
            ConsoleMeta(
              "4444",
              fakeChatId.id,
              Instant.ofEpochSecond(15*60)
            )
          )
        )

      Then("the instructions should be reported")
      result shouldEqual List(
        SendText(fakeChatId, "Сервер создан. Скопируй в консоль это"),
        Sleep(200.millis),
        SendText(fakeChatId, "`connect 127.0.0.1:27015; password 4444`",Some(Markdown))
      )
    }
  }
}
