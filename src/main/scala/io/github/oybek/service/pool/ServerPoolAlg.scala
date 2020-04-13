package io.github.oybek.service.pool

import java.sql.Timestamp

import io.github.oybek.domain.Server

trait ServerPoolAlg[F[_]] {
  def init: F[Unit]
  def info: F[List[String]]
  def find(chatId: Long): F[Option[Server[F]]]
  def poll(chatId: Long, rentUntil: Timestamp, map: String): F[Either[ServerPoolError, Server[F]]]
  def free(chatId: Long): F[Either[ServerPoolError, Unit]]
}
