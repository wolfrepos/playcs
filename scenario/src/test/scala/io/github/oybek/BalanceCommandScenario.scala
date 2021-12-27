package io.github.oybek

import io.github.oybek.fakes.FakeData.fakeChatId
import io.github.oybek.model.Reaction.SendText
import io.github.oybek.setup.ConsoleSetup
import org.scalatest.GivenWhenThen
import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

class BalanceCommandScenario extends AnyFeatureSpec with GivenWhenThen with ConsoleSetup {

  info("As a user")
  info("I want to be able to see the status of the server pool")

  Feature("/balance command") {
    Scenario("User gives command '/balance'") {
      Given("console")
      When("/balance command received")
      Then("command to status is returned")
      console.handle(fakeChatId, "/balance") shouldEqual
        List(SendText(fakeChatId, "Свободных серверов: 1"))
    }

    Scenario("User gives command '/balance' when there is not free servers") {
      Given("console")
      console.handle(fakeChatId, "/new")
      When("/balance command received")
      Then("command to status is returned")
      console.handle(fakeChatId, "/balance") shouldEqual
        List(SendText(fakeChatId, "Свободных серверов: 0"))
    }
  }
}
