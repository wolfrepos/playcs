package io.github.oybek.playcs.dto

import telegramium.bots.{ChatIntId, ParseMode}

import scala.concurrent.duration.FiniteDuration

enum Reaction:
  case SendText(
      chatId: ChatIntId,
      text: String,
      parseMode: Option[ParseMode] = None
  ) extends Reaction
  case Sleep(finiteDuration: FiniteDuration) extends Reaction
  case Receive(chatId: ChatIntId, text: String) extends Reaction
