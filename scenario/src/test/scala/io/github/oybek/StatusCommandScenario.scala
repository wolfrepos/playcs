package io.github.oybek

import io.github.oybek.fakes.FakeData.fakeChatId
import io.github.oybek.model.Reaction.SendText
import io.github.oybek.setup.ConsoleSetup
import org.scalatest.GivenWhenThen
import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

class StatusCommandScenario extends AnyFeatureSpec with GivenWhenThen with ConsoleSetup {

  info("As a user")
  info("I want to be able to see the status of the server pool")

  Feature("/status command") {
    Scenario("User gives command '/status'") {
      Given("console")
      When("/status command received")
      Then("command to status is returned")
      console.handle(fakeChatId, "/status") shouldEqual
        List(SendText(fakeChatId, "Свободных серверов: 1"))
    }

    Scenario("User gives command '/status' when there is not free servers") {
      Given("console")
      console.handle(fakeChatId, "/new")
      When("/status command received")
      Then("command to status is returned")
      console.handle(fakeChatId, "/status") shouldEqual
        List(SendText(fakeChatId, "Свободных серверов: 0"))
    }
  }
}
