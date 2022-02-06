package io.github.oybek.service

import io.github.oybek.common.WithMeta
import io.github.oybek.model.ConsoleMeta
import telegramium.bots.ChatIntId

import scala.concurrent.duration.FiniteDuration

trait HldsConsolePoolManager[F[_]]:
  def rentConsole(chatId: ChatIntId, ttl: FiniteDuration): F[HldsConsole[F] WithMeta ConsoleMeta]
  def freeConsole(chatIds: ChatIntId*): F[Unit]
  def findConsole(chatId: ChatIntId): F[Option[HldsConsole[F] WithMeta ConsoleMeta]]
  def getConsolesWith(prop: ConsoleMeta => Boolean): F[List[HldsConsole[F] WithMeta ConsoleMeta]]
