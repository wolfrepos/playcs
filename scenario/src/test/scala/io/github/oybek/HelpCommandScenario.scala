package io.github.oybek

import io.github.oybek.cstrike.model.Command.helpText
import io.github.oybek.fakes.FakeData.fakeChatId
import io.github.oybek.model.Reaction.SendText
import io.github.oybek.setup.ConsoleSetup
import org.scalatest.GivenWhenThen
import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

class HelpCommandScenario extends AnyFeatureSpec with GivenWhenThen with ConsoleSetup {

  info("As a user")
  info("I want to be able to get help message")

  Feature("/help command") {
    Scenario("User gives command '/help'") {
      Given("console")
      When("/help command received")
      Then("help message is returned")
      console.handle(fakeChatId, "/help") shouldEqual Right(List(SendText(fakeChatId, helpText)))
    }
  }
}
