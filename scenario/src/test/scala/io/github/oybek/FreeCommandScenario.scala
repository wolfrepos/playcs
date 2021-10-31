package io.github.oybek

import io.github.oybek.fakes.FakeData.fakeChatId
import io.github.oybek.model.Reaction.SendText
import io.github.oybek.setup.ConsoleSetup
import org.scalatest.GivenWhenThen
import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

class FreeCommandScenario extends AnyFeatureSpec with GivenWhenThen with ConsoleSetup {

  info("As a user")
  info("I want to be able to free the server used by us")

  Feature("/free command") {
    Scenario("User gives command '/free' where there is no created server") {
      Given("console with allocated server")

      When("/free command received")

      Then("server is free and returned back to pool and appropriate message returned")
      console.handle(fakeChatId, "/free") shouldEqual
        List(SendText(fakeChatId, "Сервер освобожден"))
    }

    Scenario("User gives command '/free'") {
      Given("console with allocated server")
      console.handle(fakeChatId, "/new")

      When("/free command received")

      Then("server is free and returned back to pool and appropriate message returned")
      console.handle(fakeChatId, "/free") shouldEqual
        List(SendText(fakeChatId, "Сервер освобожден"))
    }
  }
}
