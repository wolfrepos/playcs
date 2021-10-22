package io.github.oybek.service

import io.github.oybek.model.Reaction
import telegramium.bots.ChatIntId

trait Console[F[_]] {
  def handle(chatId: ChatIntId, text: String): F[List[Reaction]]
}
