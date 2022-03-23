package io.github.oybek.database.dao.impl

import doobie.*
import doobie.implicits.*
import io.github.oybek.database.dao.BalanceDao
import io.github.oybek.database.model.Balance

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

given getFiniteDuration: Get[FiniteDuration] =
  Get[Long].map(FiniteDuration(_, TimeUnit.MINUTES))

object BalanceDaoImpl extends BalanceDao[ConnectionIO]:
  override def add(balance: Balance): ConnectionIO[Int] =
    import balance.*
    sql"""
         |insert into balance (telegram_id, minutes)
         |values (${telegramId.id}, ${timeLeft.toMinutes})
         |""".stripMargin.update.run

  override def update(balance: Balance): ConnectionIO[Int] =
    import balance.*
    sql"""
         |update balance set minutes = ${timeLeft.toMinutes}
         |where telegram_id = ${telegramId.id}
         |""".stripMargin.update.run

  override def findBy(telegramId: Long): ConnectionIO[Option[Balance]] =
    sql"""
         |select telegram_id, minutes from balance
         |where telegram_id = $telegramId
         |""".stripMargin.query[Balance].option

  override def addIfNotExists(balance: Balance): ConnectionIO[Int] =
    import balance.*
    sql"""
         |insert into balance (telegram_id, minutes)
         |values (${telegramId.id}, ${timeLeft.toMinutes})
         |on conflict (telegram_id) do nothing
         |""".stripMargin.update.run
