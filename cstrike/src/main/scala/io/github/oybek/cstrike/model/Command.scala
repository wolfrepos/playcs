package io.github.oybek.cstrike.model

import io.github.oybek.cstrike.model.Command.helpText

import java.time.OffsetDateTime
import scala.concurrent.duration.Duration
import scala.concurrent.duration.FiniteDuration

import concurrent.duration.DurationInt

enum Command(val command: String, val description: String):
  case NewCommand(map: Option[String]) extends Command("/new", "create a server, example: /new de_dust")
  case FreeCommand extends Command("/free", "delete the server")
  case BalanceCommand extends Command("/balance", "show balance")
  case HelpCommand extends Command("/help", "show this message")
  case SayCommand(text: String) extends Command("/say", "write message to game")
  case IncreaseBalanceCommand(telegramId: Long, duration: FiniteDuration) extends Command("/balance", "increase balance [admin]")
  case WillCommand(hours: List[OffsetDateTime]) extends Command("/will", "create a will")

object Command:

  val visible: List[Command] = List(
    NewCommand(None),
    FreeCommand,
    WillCommand(List.empty[OffsetDateTime]),
    HelpCommand,
  )

  val helpText: String = visible 
    .map(x => x.command + " - " + x.description)
    .mkString("\n")
