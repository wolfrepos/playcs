package io.github.oybek.service

import io.github.oybek.common.logger.{Context, ContextData}
import io.github.oybek.model.Reaction
import telegramium.bots.ChatIntId

trait Console[F[_]]:
  def handle(chatId: ChatIntId, text: String): Context[F[List[Reaction]]]
  def expireCheck(using contextData: ContextData): Context[F[Unit]]
