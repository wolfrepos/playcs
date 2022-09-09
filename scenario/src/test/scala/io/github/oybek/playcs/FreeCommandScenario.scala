package io.github.oybek.playcs

import io.github.oybek.playcs.fakes.FakeData.fakeChatId
import io.github.oybek.playcs.fakes.FakeData.fakeUser
import io.github.oybek.playcs.dto.Reaction.SendText
import io.github.oybek.playcs.setup.HubSetup
import org.scalatest.GivenWhenThen
import org.scalatest.featurespec.AnyFeatureSpec

class FreeCommandScenario extends AnyFeatureSpec with GivenWhenThen with HubSetup:

  info("As a user")
  info("I want to be able to free the server used by us")

  Feature("/free command") {
    Scenario("User gives command '/free' where there is no created server") {
      Given("console with allocated server")

      When("/free command received")

      Then(
        "server is free and returned back to pool and appropriate message returned"
      )
      assert(
        hub.handle(fakeChatId, fakeUser, "/free") ===
          Right(List(SendText(fakeChatId, "No created servers")))
      )
    }

    Scenario("User gives command '/free'") {
      Given("console with allocated server")
      hub.handle(fakeChatId, fakeUser, "/new")

      When("/free command received")

      Then(
        "server is free and returned back to pool and appropriate message returned"
      )
      assert(
        hub.handle(fakeChatId, fakeUser, "/free") ===
          Right(List(SendText(fakeChatId, "Server has been deleted")))
      )
    }
  }
