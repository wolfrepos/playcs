package io.github.oybek.service

import io.github.oybek.common.WithMeta
import io.github.oybek.model.ConsoleMeta

import scala.concurrent.duration.FiniteDuration

trait HldsConsolePoolManager[F[_]] {
  def expireCheck: F[Unit]
  def rentConsole(chatId: Long,
                  ttl: FiniteDuration): F[Either[String, HldsConsole[F] WithMeta ConsoleMeta]]
  def freeConsole(chatId: Long): F[Unit]
  def findConsole(chatId: Long): F[Option[HldsConsole[F] WithMeta ConsoleMeta]]
  def status: F[String]
}
