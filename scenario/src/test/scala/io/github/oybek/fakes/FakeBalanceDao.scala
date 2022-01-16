package io.github.oybek.fakes

import cats.Applicative
import cats.implicits.catsSyntaxApplicativeId
import io.github.oybek.database.dao.BalanceDao
import io.github.oybek.database.model.Balance
import telegramium.bots.ChatIntId

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

class FakeBalanceDao[F[_]: Applicative] extends BalanceDao[F] {
  override def findBy(telegramId: Long): F[Option[Balance]] =
    Option(Balance(ChatIntId(123), FiniteDuration(15*60, TimeUnit.SECONDS))).pure[F]

  override def addOrUpdate(balance: Balance): F[Int] =
    1.pure[F]

  override def addIfNotExists(balance: Balance): F[Int] =
    1.pure[F]
}
