package io.github.oybek.playcs

import io.github.oybek.playcs.dto.Reaction.*
import io.github.oybek.playcs.fakes.FakeData.anotherFakeChatId
import io.github.oybek.playcs.fakes.FakeData.fakeChatId
import io.github.oybek.playcs.fakes.FakeData.fakePassword
import io.github.oybek.playcs.fakes.FakeData.fakeUser
import io.github.oybek.playcs.setup.HubSetup
import org.scalatest.GivenWhenThen
import org.scalatest.featurespec.AnyFeatureSpec
import telegramium.bots.Markdown

import java.time.Instant
import scala.concurrent.duration.DurationInt

class NewCommandSpec extends AnyFeatureSpec with GivenWhenThen with HubSetup:

  info("As a user")
  info("I want to be able to create dedicated counter strike server")

  Feature("/new command") {
    Scenario("User gives command '/new'") {
      fakeHlds.reset

      Given("console which has a free dedicated servers")

      When("/new command received")
      val result = hub.handle(fakeChatId, "/new")

      Then("new dedicated server should be created")
      assert(
        fakeHlds.getCalledCommands ===
          List(
            s"sv_password $fakePassword",
            "map de_dust2",
            s"sv_password $fakePassword",
            "changelevel de_dust2"
          )
      )

      Then("the instructions should be reported")
      assert(
        result === Right(
          List(
            SendText(
              fakeChatId,
              "Your server is created " +
                s"(after ${hldsTimeout.toMinutes} minutes it will be deleted). " +
                "Copy paste next line to game console"
            ),
            Sleep(200.millis),
            SendText(
              fakeChatId,
              "`connect 127.0.0.1:27015; password 4444`",
              Some(Markdown)
            ),
            Sleep(hldsTimeout),
            Receive(fakeChatId, "/free")
          )
        )
      )
    }

    Scenario("User gives command '/new' already having the server") {
      fakeHlds.reset

      Given("user who already has the server")

      When("/new command received")
      val result = hub.handle(fakeChatId, "/new")

      Then("new dedicated server should be created")
      assert(fakeHlds.getCalledCommands === List("changelevel de_dust2"))

      Then("the instructions should be reported")
      assert(
        result === Right(
          List(
            SendText(
              fakeChatId,
              "You already got the server, just changing a map"
            )
          )
        )
      )
    }

    Scenario("User gives command '/new' when there is no free server") {
      fakeHlds.reset

      Given("console which has no free dedicated servers")

      When("/new command received")
      val result = hub.handle(anotherFakeChatId, "/new")

      Then("No servers left message should be returned")
      assert(fakeHlds.getCalledCommands.isEmpty)
      assert(
        result === Right(
          List(
            SendText(
              anotherFakeChatId,
              "No free server left, contact t.me/turtlebots"
            )
          )
        )
      )
    }
  }
