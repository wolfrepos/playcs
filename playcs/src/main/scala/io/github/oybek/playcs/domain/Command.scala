package io.github.oybek.playcs.domain

import atto.*
import atto.Atto.*
import atto.Parser.ParserMonad
import cats.implicits.*
import io.github.oybek.playcs.domain.Command.helpText

import java.time.OffsetDateTime
import scala.concurrent.duration.{Duration, DurationInt, FiniteDuration}

enum Command(val command: String, val description: String):
  case NewCommand(map: Option[String]) extends Command("/new", "create a server, example: /new de_dust")
  case FreeCommand extends Command("/free", "delete the server")
  case HelpCommand extends Command("/help", "show this message")
  case SayCommand(text: String) extends Command("/say", "write message to game")

object Command:
  def parse(text: String): String | Command =
    commandParser.parseOnly(text).either match {
      case Left(error)    => error
      case Right(command) => command
    }

  val visible: List[Command] = List(
    NewCommand(None),
    FreeCommand,
    HelpCommand
  )

  val helpText: String = visible
    .map(x => x.command + " - " + x.description)
    .mkString("\n")

  private val mapNameParser: Parser[String] =
    stringOf(digit | letter | char('_'))

  private val newCommandParser: Parser[NewCommand] =
    (string(NewCommand(None).command) ~> optSuffix ~> opt(ws1 ~> mapNameParser))
      .map(map => NewCommand(map))

  private val helpCommandParser: Parser[HelpCommand.type] =
    (string(HelpCommand.command) ~> optSuffix).map(_ => HelpCommand)

  private val freeCommandParser: Parser[FreeCommand.type] =
    (string(FreeCommand.command) ~> optSuffix).map(_ => FreeCommand)

  private def commandParser: Parser[Command] =
    ws ~> (
      newCommandParser |
        helpCommandParser |
        freeCommandParser |
        err[Command]("Unknown command")
    ) <~ ws <~ endOfInput

  private lazy val optSuffix = opt(string("@playcs_bot"))
  private lazy val ws1 = many1(whitespace)
  private lazy val ws = many(whitespace)
