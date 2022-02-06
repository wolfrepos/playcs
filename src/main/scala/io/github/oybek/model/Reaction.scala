package io.github.oybek.model

import telegramium.bots.{ChatIntId, ParseMode}

import scala.concurrent.duration.FiniteDuration

sealed trait Reaction
object Reaction:
  case class SendText(chatId: ChatIntId, text: String, parseMode: Option[ParseMode] = None) extends Reaction
  case class Sleep(finiteDuration: FiniteDuration) extends Reaction
