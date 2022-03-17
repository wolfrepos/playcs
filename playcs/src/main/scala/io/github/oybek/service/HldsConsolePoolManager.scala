package io.github.oybek.service

import io.github.oybek.common.WithMeta
import io.github.oybek.common.logger.Context
import io.github.oybek.model.ConsoleMeta
import telegramium.bots.ChatIntId

import scala.concurrent.duration.FiniteDuration

trait HldsConsolePoolManager[F[_]]:
  def rentConsole(chatId: ChatIntId, ttl: FiniteDuration): Context[F[HldsConsole[F] WithMeta ConsoleMeta]]
  def freeConsole(chatIds: ChatIntId*): Context[F[Unit]]
  def findConsole(chatId: ChatIntId): Context[F[Option[HldsConsole[F] WithMeta ConsoleMeta]]]
  def getConsolesWith(prop: ConsoleMeta => Boolean): Context[F[List[HldsConsole[F] WithMeta ConsoleMeta]]]
