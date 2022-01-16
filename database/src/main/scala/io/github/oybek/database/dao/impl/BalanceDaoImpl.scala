package io.github.oybek.database.dao.impl

import doobie._
import doobie.implicits._
import io.github.oybek.database.dao.BalanceDao
import io.github.oybek.database.model.Balance

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

object BalanceDaoImpl extends BalanceDao[ConnectionIO] {

  override def addOrUpdate(balance: Balance): ConnectionIO[Int] = {
    import balance._
    sql"""
         |insert into balance (telegram_id, seconds)
         |values (${telegramId.id}, ${timeLeft.toSeconds})
         |on conflict (telegram_id) do
         |update set seconds = ${timeLeft.toSeconds}
         |""".stripMargin.update.run
  }

  implicit val getFiniteDuration: Get[FiniteDuration] =
    Get[Long].map(FiniteDuration(_, TimeUnit.SECONDS))

  override def findBy(telegramId: Long): ConnectionIO[Option[Balance]] =
    sql"""
         |select telegram_id, seconds from balance
         |where telegram_id = $telegramId
         |""".stripMargin.query[Balance].option

  override def addIfNotExists(balance: Balance): ConnectionIO[Int] = {
    import balance._
    sql"""
         |insert into balance (telegram_id, seconds)
         |values (${telegramId.id}, ${timeLeft.toSeconds})
         |on conflict (telegram_id) do nothing
         |""".stripMargin.update.run
  }
}
