package io.github.oybek.model

import telegramium.bots.ChatIntId

import java.time.Instant

case class ConsoleMeta(password: String,
                       usingBy: ChatIntId,
                       deadline: Instant)
