package io.github.oybek.cstrike.service.impl

import atto.Atto._
import atto._
import io.github.oybek.cstrike.model.Command
import io.github.oybek.cstrike.model.Command.{FreeCommand, HelpCommand, JoinCommand, MapsCommand, NewCommand, StatusCommand}
import io.github.oybek.cstrike.service.Translator

import scala.concurrent.duration.{DurationInt, FiniteDuration}

class TranslatorImpl extends Translator {
  override def translate(text: String): Either[String, Command] =
    commandParser.parseOnly(text).either

  private val durationParser: Parser[FiniteDuration] =
    int.map(_.minutes)

  private val mapNameParser: Parser[String] =
    stringOf(digit | letter | char('_'))

  private val newCommandParser: Parser[NewCommand] =
    (string("/new") ~> optSuffix ~> ws1 ~> mapNameParser ~ opt(ws1 ~> durationParser)).map {
      case (map, duration) => NewCommand(map, duration.getOrElse(30.minutes))
    }

  private val mapsCommandParser: Parser[MapsCommand.type] =
    (string("/maps") ~> optSuffix).map(_ => MapsCommand)

  private val joinCommandParser: Parser[JoinCommand.type] =
    (string("/join") ~> optSuffix).map(_ => JoinCommand)

  private val statusCommandParser: Parser[StatusCommand.type] =
    (string("/status") ~> optSuffix).map(_ => StatusCommand)

  private val helpCommandParser: Parser[HelpCommand.type] =
    (string("/help") ~> optSuffix).map(_ => HelpCommand)

  private val freeCommandParser: Parser[FreeCommand.type] =
    (string("/free") ~> optSuffix).map(_ => FreeCommand)

  private val commandParser: Parser[Command] =
    ws ~> (
      newCommandParser |
      mapsCommandParser |
      joinCommandParser |
      statusCommandParser |
      helpCommandParser |
      freeCommandParser |
      err[Command]("Unknown command")
    ) <~ ws <~ endOfInput

  private lazy val optSuffix = opt(string("@playcs_bot"))
  private lazy val ws1 = many1(whitespace)
  private lazy val ws = many(whitespace)
}
