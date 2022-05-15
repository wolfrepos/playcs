package io.github.oybek.cstrike.parser.impl

import atto.Atto.*
import atto.Parser.ParserMonad
import atto.*
import cats.implicits.*
import io.github.oybek.cstrike.model.Command
import io.github.oybek.cstrike.model.Command.*
import io.github.oybek.cstrike.parser.CommandParser

import java.time.OffsetDateTime
import java.time.ZoneOffset
import scala.concurrent.duration.DurationLong
import scala.util.Try

class CommandParserImpl extends CommandParser:
  override def parse(text: String, year: Int): String | Command =
    commandParser(year).parseOnly(text).either match {
      case Left(error) => error
      case Right(command) => command
    }

  private val mapNameParser: Parser[String] =
    stringOf(digit | letter | char('_'))

  private val newCommandParser: Parser[NewCommand] =
    (string(NewCommand(None).command) ~> optSuffix ~> opt(ws1 ~> mapNameParser))
      .map(map => NewCommand(map))

  private val balanceCommandParser: Parser[BalanceCommand.type] =
    (string(BalanceCommand.command) ~> optSuffix).map(_ => BalanceCommand)

  private val helpCommandParser: Parser[HelpCommand.type] =
    (string(HelpCommand.command) ~> optSuffix).map(_ => HelpCommand)

  private val freeCommandParser: Parser[FreeCommand.type] =
    (string(FreeCommand.command) ~> optSuffix).map(_ => FreeCommand)

  private val sayCommandParser: Parser[SayCommand] =
    (string(SayCommand("").command) ~> optSuffix ~> ws1 ~> stringOf1(anyChar))
      .map(text => SayCommand(text))

  private val increaseBalanceCommandParser: Parser[IncreaseBalanceCommand] =
    (string(BalanceCommand.command) ~> optSuffix ~> ws1 ~> long ~ (ws1 ~> long)).map {
      case (telegramId, duration) => IncreaseBalanceCommand(telegramId, duration.minutes)
    }

  private def commandParser(year: Int): Parser[Command] =
    ws ~> (
      newCommandParser |
      increaseBalanceCommandParser |
      balanceCommandParser |
      helpCommandParser |
      freeCommandParser |
      sayCommandParser |
      err[Command]("Unknown command")
    ) <~ ws <~ endOfInput

  private lazy val optSuffix = opt(string("@playcs_bot"))
  private lazy val ws1 = many1(whitespace)
  private lazy val ws = many(whitespace)
