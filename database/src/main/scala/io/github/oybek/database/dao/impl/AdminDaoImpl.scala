package io.github.oybek.database.dao.impl

import doobie.*
import doobie.implicits.*
import io.github.oybek.database.dao.AdminDao

object AdminDaoImpl extends AdminDao[ConnectionIO]:
  override def isAdmin(telegramId: Long): ConnectionIO[Boolean] =
    sql"select exists(select 1 from admin where telegram_id = $telegramId)".query[Boolean].unique
