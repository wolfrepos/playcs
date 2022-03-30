package io.github.oybek.database.hlds.dao

import io.github.oybek.database.hlds.model.Hlds

trait HldsDao[F[_]]:
  def add(hlds: Hlds): F[Unit]
  def exists(chatId: Long): F[Boolean]
  def delete(chatId: Long): F[Unit]
  def all: F[List[Hlds]]
