package io.github.oybek.database.dao

import io.github.oybek.database.model.Balance

trait BalanceDao[F[_]] {
  def findBy(telegramId: Long): F[Option[Balance]]
  def addOrUpdate(balance: Balance): F[Int]
}
