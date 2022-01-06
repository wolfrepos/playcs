package io.github.oybek.service

import io.github.oybek.common.WithMeta
import io.github.oybek.model.ConsoleMeta

trait HldsConsolePoolManager[F[_]] {
  def expireCheck: F[Unit]
  def rentConsole(chatId: Long): F[HldsConsole[F] WithMeta ConsoleMeta]
  def freeConsole(chatIds: Long*): F[Unit]
  def findConsole(chatId: Long): F[Option[HldsConsole[F] WithMeta ConsoleMeta]]
}
