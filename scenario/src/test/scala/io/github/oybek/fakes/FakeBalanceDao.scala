package io.github.oybek.fakes

import cats.Applicative
import cats.implicits.catsSyntaxApplicativeId
import io.github.oybek.database.dao.BalanceDao
import io.github.oybek.database.model.Balance

class FakeBalanceDao[F[_]: Applicative] extends BalanceDao[F] {
  override def findBy(telegramId: Long): F[Option[Balance]] =
    Option(Balance(123, 15*60)).pure[F]

  override def addOrUpdate(balance: Balance): F[Int] =
    1.pure[F]

  override def addIfNotExists(balance: Balance): F[Int] =
    1.pure[F]
}
