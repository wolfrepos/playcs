package io.github.oybek

import io.github.oybek.common.logger.ContextData
import io.github.oybek.exception.BusinessException.UnathorizedException
import io.github.oybek.fakes.FakeData.{adminChatId, fakeChatId}
import io.github.oybek.model.Reaction.{SendText, Sleep}
import io.github.oybek.setup.ConsoleSetup
import org.scalatest.GivenWhenThen
import org.scalatest.featurespec.AnyFeatureSpec

import scala.concurrent.duration.DurationInt

class IncreaseBalanceCommandScenario extends AnyFeatureSpec with GivenWhenThen with ConsoleSetup:

  info("As an admin")
  info("I want to be able to increase the balance of the chat")

  Feature("/balance command") {
    Scenario("Admin gives command '/balance'") {
      Given("console")
      When("/balance command received")
      Then("balance of chat increased, admin and chat are informed")
      assert(console.handle(adminChatId, s"/balance ${fakeChatId.id} 30") ===
        Right(List(
          SendText(adminChatId, s"Chat ${fakeChatId.id} balance increased to 2700 seconds"),
          SendText(fakeChatId, "Your balance increased to 2700 seconds")
        )))
    }

    Scenario("User gives command '/balance'") {
      Given("console")
      console.handle(fakeChatId, "/new")
      When("/balance command received")
      Then("nothing happens")
      assert(
        console.handle(fakeChatId, s"/balance ${fakeChatId.id} 30") === Left(UnathorizedException)
      )
    }
  }
