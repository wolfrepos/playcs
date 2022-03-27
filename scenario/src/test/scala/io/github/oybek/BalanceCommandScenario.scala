package io.github.oybek

import io.github.oybek.common.logger.ContextData
import io.github.oybek.fakes.FakeData.fakeChatId
import io.github.oybek.fakes.FakeData.fakeUser
import io.github.oybek.model.Reaction.{SendText, Sleep}
import io.github.oybek.setup.HubSetup
import org.scalatest.GivenWhenThen
import org.scalatest.featurespec.AnyFeatureSpec

import scala.concurrent.duration.DurationInt

class BalanceCommandScenario extends AnyFeatureSpec with GivenWhenThen with HubSetup:

  info("As a user")
  info("I want to be able to see the status of the server pool")

  Feature("/balance command") {
    Scenario("User gives command '/balance'") {
      Given("console")
      When("/balance command received")
      Then("command to status is returned")
      /*
      assert(hub.handle(fakeChatId, "/balance") ===
        Right(List(
          SendText(fakeChatId,
            s"""
               |Your balance: 15 minutes
               |Transfer some money by link below and get minutes (1 rub. = 5 minutes)
               |https://www.tinkoff.ru/rm/khashimov.oybek1/Cc3Jm91036
               |Be sure to include the following code in your message when transferring 
               |""".stripMargin),
          Sleep(500.millis),
          SendText(fakeChatId, fakeChatId.id.toString)
        )))
      */
    }

    Scenario("User gives command '/balance' when there is not free servers") {
      Given("console")
      hub.handle(fakeChatId, fakeUser, "/new")
      When("/balance command received")
      Then("command to status is returned")
      /*
      assert(hub.handle(fakeChatId, "/balance") ===
        Right(List(
          SendText(fakeChatId,
            s"""
               |Your balance: 15 minutes
               |Transfer some money by link below and get minutes (1 rub. = 5 minutes)
               |https://www.tinkoff.ru/rm/khashimov.oybek1/Cc3Jm91036
               |Be sure to include the following code in your message when transferring 
               |""".stripMargin),
          Sleep(500.millis),
          SendText(fakeChatId, fakeChatId.id.toString))
        ))
      */
    }
  }
