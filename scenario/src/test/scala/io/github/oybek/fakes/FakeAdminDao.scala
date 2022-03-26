package io.github.oybek.fakes

import cats.Applicative
import cats.implicits.catsSyntaxApplicativeId
import io.github.oybek.database.admin.dao.AdminDao
import io.github.oybek.database.balance.model.Balance
import io.github.oybek.fakes.FakeData.adminChatId
import telegramium.bots.ChatIntId

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

class FakeAdminDao[F[_]: Applicative] extends AdminDao[F]:
  override def isAdmin(telegramId: Long): F[Boolean] =
    (telegramId == adminChatId.id).pure[F]
