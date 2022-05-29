package io.github.oybek.cstrike.parser

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

trait CommandParser:
  def parse(text: String): String | Command

object CommandParser extends CommandParser:
  override def parse(text: String): String | Command =
    commandParser.parseOnly(text).either match {
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

  private def commandParser: Parser[Command] =
    ws ~> (
      newCommandParser |
      balanceCommandParser |
      helpCommandParser |
      freeCommandParser |
      err[Command]("Unknown command")
    ) <~ ws <~ endOfInput

  private lazy val optSuffix = opt(string("@playcs_bot"))
  private lazy val ws1 = many1(whitespace)
  private lazy val ws = many(whitespace)
