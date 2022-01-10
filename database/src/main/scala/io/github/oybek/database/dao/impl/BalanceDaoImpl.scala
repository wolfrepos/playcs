package io.github.oybek.database.dao.impl

import doobie._
import doobie.implicits._
import io.github.oybek.database.dao.BalanceDao
import io.github.oybek.database.model.Balance

object BalanceDaoImpl extends BalanceDao[ConnectionIO] {

  override def addOrUpdate(balance: Balance): ConnectionIO[Int] = {
    import balance._
    sql"""
         |insert into balance (telegram_id, seconds)
         |values ($telegramId, $seconds)
         |on conflict (telegram_id) do
         |update set seconds = $seconds
         |""".stripMargin.update.run
  }

  override def findBy(telegramId: Long): ConnectionIO[Option[Balance]] =
    sql"""
         |select telegram_id, seconds from balance
         |where telegram_id = $telegramId
         |""".stripMargin.query[Balance].option

  override def addIfNotExists(balance: Balance): ConnectionIO[Int] = {
    import balance._
    sql"""
         |insert into balance (telegram_id, seconds)
         |values ($telegramId, $seconds)
         |on conflict (telegram_id) do nothing
         |""".stripMargin.update.run
  }
}
