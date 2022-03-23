package io.github.oybek

import io.github.oybek.common.With
import io.github.oybek.common.and
import io.github.oybek.exception.BusinessException.NoFreeConsolesException
import io.github.oybek.fakes.FakeData.anotherFakeChatId
import io.github.oybek.fakes.FakeData.fakeChatId
import io.github.oybek.fakes.FakeData.fakePassword
import io.github.oybek.model.Reaction.SendText
import io.github.oybek.model.Reaction.Sleep
import io.github.oybek.setup.ConsoleSetup
import org.scalatest.GivenWhenThen
import org.scalatest.featurespec.AnyFeatureSpec
import telegramium.bots.Markdown

import java.time.Instant
import scala.concurrent.duration.DurationInt

class NewCommandSpec extends AnyFeatureSpec with GivenWhenThen with ConsoleSetup:

  info("As a user")
  info("I want to be able to create dedicated counter strike server")

  Feature("/new command") {
    Scenario("User gives command '/new'") {
      Given("console which has a free dedicated servers")

      When("/new command received")
      val result = console.handle(fakeChatId, "/new")

      Then("new dedicated server should be created")
      assert(
        hldsConsole.getCalledCommands ===
          List(
            s"sv_password $fakePassword",
            "map de_dust2",
            s"sv_password $fakePassword",
            "changelevel de_dust2"
          )
      )
      assert(consolePoolRef.get === Right((Nil, List(hldsConsole and fakeChatId))))

      Then("the instructions should be reported")
      assert(result === Right(List(
        SendText(fakeChatId, "Your server is ready. Copy paste this"),
        Sleep(200.millis),
        SendText(fakeChatId, "`connect 127.0.0.1:27015; password 4444`",Some(Markdown)))))
    }

    Scenario("User gives command '/new' when there is no free server") {
      Given("console which has no free dedicated servers")

      When("/new command received")
      val result = console.handle(anotherFakeChatId, "/new")

      Then("No servers left message should be returned")
      assert(result === Right(
        List(SendText(anotherFakeChatId, "No free server left, contact t.me/turtlebots"))))
    }
  }
