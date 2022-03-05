package io.github.oybek.cstrike.model

import io.github.oybek.cstrike.model.Command.helpText

import scala.concurrent.duration.Duration

enum Command(val command: String, val description: String):
  case NewCommand(map: Option[String]) extends Command("/new", "создать сервер, пример: /new de_dust")
  case FreeCommand extends Command("/free", "удалить сервер")
  case MapsCommand extends Command("/maps", "список доступных карт")
  case JoinCommand extends Command("/join", "напомнить как подключаться")
  case BalanceCommand extends Command("/balance", "показать баланс")
  case HelpCommand extends Command("/help", "вывести это сообщение")
  case SayCommand(text: String) extends Command("/say", "написать сообщение игрокам")
  case IncreaseBalanceCommand(telegramId: Long, duration: Duration) extends Command("/balance", "пополнить баланс")

object Command:

  val all: List[Command] = List(
    NewCommand(None),
    FreeCommand,
    MapsCommand,
    JoinCommand,
    BalanceCommand,
    HelpCommand,
    SayCommand("")
  )

  val helpText: String = all
    .map(x => x.command + " - " + x.description)
    .mkString("\n")
