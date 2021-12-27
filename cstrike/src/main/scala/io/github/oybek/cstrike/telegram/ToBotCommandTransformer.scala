package io.github.oybek.cstrike.telegram

import io.github.oybek.cstrike.model.Command
import io.scalaland.chimney.Transformer
import telegramium.bots.BotCommand

object ToBotCommandTransformer {
  implicit val commandToBotCommand: Transformer[Command, BotCommand] =
    Transformer.define[Command, BotCommand]
      .withFieldComputed(_.command, _.command)
      .withFieldComputed(_.description, _.description)
      .buildTransformer
}
