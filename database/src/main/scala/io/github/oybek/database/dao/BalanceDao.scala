package io.github.oybek.database.dao

import doobie.*
import doobie.implicits.*
import io.github.oybek.database.dao.BalanceDao
import io.github.oybek.database.model.Balance

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

trait BalanceDao[F[_]]:
  def findBy(telegramId: Long): F[Option[Balance]]
  def upsert(balance: Balance): F[Int]

given getFiniteDuration: Get[FiniteDuration] =
  Get[Long].map(FiniteDuration(_, TimeUnit.MINUTES))

object BalanceDao:
  def create = new BalanceDao[ConnectionIO]:
    override def upsert(balance: Balance): ConnectionIO[Int] =
      import balance.*
      sql"""
          |insert into balance (chat_id, minutes)
          |values (${telegramId.id}, ${timeLeft.toMinutes})
          |on conflict (chat_id) do
          |update set minutes = ${timeLeft.toMinutes}
          |""".stripMargin.update.run

    override def findBy(telegramId: Long): ConnectionIO[Option[Balance]] =
      sql"""
          |select chat_id, minutes from balance
          |where chat_id = $telegramId
          |""".stripMargin.query[Balance].option
