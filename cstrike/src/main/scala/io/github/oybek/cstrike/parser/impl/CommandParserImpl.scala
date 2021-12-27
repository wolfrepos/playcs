package io.github.oybek.cstrike.parser.impl

import atto.Atto._
import atto._
import io.github.oybek.cstrike.model.Command
import io.github.oybek.cstrike.model.Command.{BalanceCommand, FreeCommand, HelpCommand, JoinCommand, MapsCommand, NewCommand}
import io.github.oybek.cstrike.parser.CommandParser

class CommandParserImpl extends CommandParser {
  override def parse(text: String): Either[String, Command] =
    commandParser.parseOnly(text).either

  private val mapNameParser: Parser[String] =
    stringOf(digit | letter | char('_'))

  private val newCommandParser: Parser[NewCommand] =
    (string(NewCommand.command) ~> optSuffix ~> opt(ws1 ~> mapNameParser))
      .map(map => NewCommand(map.getOrElse("de_dust2")))

  private val mapsCommandParser: Parser[MapsCommand.type] =
    (string(MapsCommand.command) ~> optSuffix).map(_ => MapsCommand)

  private val joinCommandParser: Parser[JoinCommand.type] =
    (string(JoinCommand.command) ~> optSuffix).map(_ => JoinCommand)

  private val balanceCommandParser: Parser[BalanceCommand.type] =
    (string(BalanceCommand.command) ~> optSuffix).map(_ => BalanceCommand)

  private val helpCommandParser: Parser[HelpCommand.type] =
    (string(HelpCommand.command) ~> optSuffix).map(_ => HelpCommand)

  private val freeCommandParser: Parser[FreeCommand.type] =
    (string(FreeCommand.command) ~> optSuffix).map(_ => FreeCommand)

  private val commandParser: Parser[Command] =
    ws ~> (
      newCommandParser |
      mapsCommandParser |
      joinCommandParser |
      balanceCommandParser |
      helpCommandParser |
      freeCommandParser |
      err[Command]("Unknown command")
    ) <~ ws <~ endOfInput

  private lazy val optSuffix = opt(string("@playcs_bot"))
  private lazy val ws1 = many1(whitespace)
  private lazy val ws = many(whitespace)
}
