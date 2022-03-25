package io.github.oybek.cstrike.model

import concurrent.duration.DurationInt
import io.github.oybek.cstrike.model.Command.helpText

import scala.concurrent.duration.{Duration, FiniteDuration}

enum Command(val command: String, val description: String):
  case NewCommand(map: Option[String]) extends Command("/new", "create a server, example: /new de_dust")
  case FreeCommand extends Command("/free", "delete the server")
  case BalanceCommand extends Command("/balance", "show balance")
  case HelpCommand extends Command("/help", "show this message")
  case SayCommand(text: String) extends Command("/say", "write message to game")
  case IncreaseBalanceCommand(telegramId: Long, duration: FiniteDuration) extends Command("/balance", "increase balance [admin]")

object Command:

  val visible: List[Command] = List(
    NewCommand(None),
    FreeCommand,
    HelpCommand,
  )

  val helpText: String = visible 
    .map(x => x.command + " - " + x.description)
    .mkString("\n")
