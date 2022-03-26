package io.github.oybek.database.admin.dao

import doobie.*
import doobie.implicits.*

trait AdminDao[F[_]]:
  def isAdmin(telegramId: Long): F[Boolean]

object AdminDao:
  def create =
    new AdminDao[ConnectionIO]:
      override def isAdmin(telegramId: Long): ConnectionIO[Boolean] =
        sql"select exists(select 1 from admin where chat_id = $telegramId)".query[Boolean].unique