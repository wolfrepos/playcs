package io.github.oybek.cstrike.model

import scala.concurrent.duration.FiniteDuration

sealed trait Command

object Command {
  case class NewCommand(map: String, ttl: FiniteDuration) extends Command
  case object FreeCommand extends Command
  case object MapsCommand extends Command
  case object JoinCommand extends Command
  case object StatusCommand extends Command
  case object HelpCommand extends Command
}
