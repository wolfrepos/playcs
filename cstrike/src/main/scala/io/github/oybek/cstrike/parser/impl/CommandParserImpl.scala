package io.github.oybek.cstrike.parser.impl

import atto.Atto.*
import atto.Parser.ParserMonad
import atto.*
import cats.implicits.catsSyntaxOptionId
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

  private def willCommandParser(year: Int): Parser[WillCommand] = (
    for
      _  <- string(WillCommand(None, None).command) <~ optSuffix <~ ws1
      args <- opt(
        for
          dd <- int 
          _  <- char('.')
          mm <- int
          _  <- ws1
          h1 <- int
          _  <- char('-')
          h2 <- int
          _  <- ws1
          os <- int
        yield (dd, mm, h1, h2, os)
      )
    yield args
  ).flatMap {
    case Some(dd, mm, h1, h2, offset) =>
      List(h1, h2).flatMap { hh =>
        Try(OffsetDateTime.of(year, mm, dd, hh, 0, 0, 0, ZoneOffset.ofHours(offset))).toOption
      } match {
        case start::end::Nil if start.isBefore(end) =>
          ParserMonad.pure[WillCommand](WillCommand(start.some, end.some))
        case _ =>
          ParserMonad.pure[WillCommand](WillCommand(None, None))
      }
    case None =>
      ParserMonad.pure[WillCommand](WillCommand(None, None))
  }

  private def commandParser(year: Int): Parser[Command] =
    ws ~> (
      newCommandParser |
      increaseBalanceCommandParser |
      balanceCommandParser |
      helpCommandParser |
      freeCommandParser |
      sayCommandParser |
      willCommandParser(year) |
      err[Command]("Unknown command")
    ) <~ ws <~ endOfInput

  private lazy val optSuffix = opt(string("@playcs_bot"))
  private lazy val ws1 = many1(whitespace)
  private lazy val ws = many(whitespace)
