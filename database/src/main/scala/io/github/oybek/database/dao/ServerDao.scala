package io.github.oybek.database.dao

import io.github.oybek.database.model.Server

trait ServerDao[F[_]]:
  def add(server: Server): F[Unit]
  def exists(chatId: Long): F[Boolean]
  def delete(chatId: Long): F[Unit]
  def all: F[List[Server]]
