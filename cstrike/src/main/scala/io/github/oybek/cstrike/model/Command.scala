package io.github.oybek.cstrike.model

sealed trait Command:
  def command: String
  def description: String

object Command {
  case class NewCommand(map: String) extends Command {
    val command: String = NewCommand.command
    val description: String = NewCommand.description
  }

  object NewCommand extends Command {
    val command: String = "/new"
    val description: String = "создать сервер, пример: /new de_dust"
  }

  case object FreeCommand extends Command {
    val command: String = "/free"
    val description: String = "удалить сервер"
  }

  case object MapsCommand extends Command {
    val command: String = "/maps"
    val description: String = "список доступных карт"
  }

  case object JoinCommand extends Command {
    val command: String = "/join"
    val description: String = "напомнить как подключаться"
  }

  case object BalanceCommand extends Command {
    val command: String = "/balance"
    val description: String = "показать баланс"
  }

  case object HelpCommand extends Command {
    val command: String = "/help"
    val description: String = "вывести это сообщение"
  }

  val all: List[Command] = List(
    NewCommand,
    FreeCommand,
    MapsCommand,
    JoinCommand,
    BalanceCommand,
    HelpCommand
  )

  val helpText: String = all
    .map(x => x.command + " - " + x.description)
    .mkString("\n")
}
