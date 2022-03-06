package io.github.oybek.database.dao

trait AdminDao[F[_]]:
  def isAdmin(telegramId: Long): F[Boolean]
