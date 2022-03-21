package io.github.oybek.database

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.dimafeng.testcontainers.{ForAllTestContainer, PostgreSQLContainer}
import doobie.*
import doobie.implicits.*
import io.github.oybek.database.config.DbConfig
import io.github.oybek.database.dao.BalanceDao
import io.github.oybek.database.dao.impl.BalanceDaoImpl
import io.github.oybek.database.model.Balance
import org.scalatest.flatspec.AnyFlatSpec
import org.testcontainers.utility.DockerImageName
import telegramium.bots.ChatIntId

import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext.global as globalEc
import scala.concurrent.duration.FiniteDuration

class BalanceDaoSpec extends AnyFlatSpec with PostgresSetup:

  val balanceDao: BalanceDao[ConnectionIO] = BalanceDaoImpl

  "addOrUpdate".should("work") in {
    transactor.use { tx =>
      for
        _ <- DB.runMigrations(tx)
        balance = Balance(telegramId = ChatIntId(123), timeLeft = FiniteDuration(60, TimeUnit.SECONDS))
        affectedRows <- balanceDao
          .add(balance)
          .transact(tx)
        _ = assert(affectedRows === 1)

        balanceOpt <- balanceDao
          .findBy(telegramId = 123)
          .transact(tx)
        _ = assert(balanceOpt === Some(balance))

        affectedRows <- balanceDao
          .add(balance)
          .transact(tx)
        _ = assert(affectedRows === 1)

        affectedRows <- balanceDao
          .add(balance)
          .transact(tx)
        _ = assert(affectedRows === 1)

        balanceOpt <- balanceDao
          .findBy(telegramId = 123)
          .transact(tx)
        _ = assert(balanceOpt === Some(balance))

        balanceOpt <- balanceDao
          .findBy(telegramId = 0)
          .transact(tx)
        _ = assert(balanceOpt === None)
      yield ()
    }.unsafeRunSync()
  }

