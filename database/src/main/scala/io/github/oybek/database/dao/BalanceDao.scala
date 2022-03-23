package io.github.oybek.database.dao

import io.github.oybek.database.model.Balance

trait BalanceDao[F[_]]:
  def findBy(telegramId: Long): F[Option[Balance]]
  def add(balance: Balance): F[Int]
  def update(balance: Balance): F[Int]
  def addIfNotExists(balance: Balance): F[Int]
