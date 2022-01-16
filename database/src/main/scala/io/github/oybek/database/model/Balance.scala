package io.github.oybek.database.model

import telegramium.bots.ChatIntId

import scala.concurrent.duration.FiniteDuration

case class Balance(telegramId: ChatIntId,
                   timeLeft: FiniteDuration)
