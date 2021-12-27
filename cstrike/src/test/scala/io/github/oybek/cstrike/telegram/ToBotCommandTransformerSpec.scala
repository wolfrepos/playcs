package io.github.oybek.cstrike.telegram

import io.github.oybek.cstrike.model.Command
import io.github.oybek.cstrike.model.Command._
import io.github.oybek.cstrike.telegram.ToBotCommandTransformer.commandToBotCommand
import io.scalaland.chimney.dsl._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers._
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.prop.TableFor2
import telegramium.bots.BotCommand

class ToBotCommandTransformerSpec extends AnyFlatSpec {

  private val tests: TableFor2[Command, BotCommand] = Table(
    ("Command", "BotCommand"),
    (BalanceCommand, BotCommand(BalanceCommand.command, BalanceCommand.description)),
    (FreeCommand, BotCommand(FreeCommand.command, FreeCommand.description)),
    (HelpCommand, BotCommand(HelpCommand.command, HelpCommand.description)),
    (JoinCommand, BotCommand(JoinCommand.command, JoinCommand.description)),
    (MapsCommand, BotCommand(MapsCommand.command, MapsCommand.description)),
    (NewCommand("de_dust2"), BotCommand(NewCommand.command, NewCommand.description)),
  )

  "translator" should "be tested" in {
    forAll(tests) {
      (command, botCommand) => command.transformInto[BotCommand] shouldEqual botCommand
    }
  }
}
