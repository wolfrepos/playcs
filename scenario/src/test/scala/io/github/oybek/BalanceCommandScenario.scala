package io.github.oybek

import io.github.oybek.fakes.FakeData.fakeChatId
import io.github.oybek.model.Reaction.{SendText, Sleep}
import io.github.oybek.setup.ConsoleSetup
import org.scalatest.GivenWhenThen
import org.scalatest.featurespec.AnyFeatureSpec

import scala.concurrent.duration.DurationInt

class BalanceCommandScenario extends AnyFeatureSpec with GivenWhenThen with ConsoleSetup {

  info("As a user")
  info("I want to be able to see the status of the server pool")

  Feature("/balance command") {
    Scenario("User gives command '/balance'") {
      Given("console")
      When("/balance command received")
      Then("command to status is returned")
      assert(console.handle(fakeChatId, "/balance") ===
        Right(List(
          SendText(fakeChatId,
            s"""
               |Ваш баланс: 15 минут
               |Для пополнения пройдите по ссылке (1 руб = 2 мин)
               |https://www.tinkoff.ru/rm/khashimov.oybek1/Cc3Jm91036
               |В сообщении при переводе обязательно укажите следующий код
               |""".stripMargin),
          Sleep(500.millis),
          SendText(fakeChatId, fakeChatId.id.toString)
        )))
    }

    Scenario("User gives command '/balance' when there is not free servers") {
      Given("console")
      console.handle(fakeChatId, "/new")
      When("/balance command received")
      Then("command to status is returned")
      assert(console.handle(fakeChatId, "/balance") ===
        Right(List(
          SendText(fakeChatId,
            s"""
               |Ваш баланс: 15 минут
               |Для пополнения пройдите по ссылке (1 руб = 2 мин)
               |https://www.tinkoff.ru/rm/khashimov.oybek1/Cc3Jm91036
               |В сообщении при переводе обязательно укажите следующий код
               |""".stripMargin),
          Sleep(500.millis),
          SendText(fakeChatId, fakeChatId.id.toString))
        ))
    }
  }
}
