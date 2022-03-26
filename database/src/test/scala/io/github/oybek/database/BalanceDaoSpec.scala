package io.github.oybek.database

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.dimafeng.testcontainers.ForAllTestContainer
import com.dimafeng.testcontainers.PostgreSQLContainer
import doobie.*
import doobie.implicits.*
import io.github.oybek.database.config.DbConfig
import io.github.oybek.database.dao.BalanceDao
import io.github.oybek.database.model.Balance
import org.testcontainers.utility.DockerImageName
import telegramium.bots.ChatIntId

import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext.global as globalEc
import scala.concurrent.duration.FiniteDuration
import org.scalatest.funsuite.AnyFunSuite

trait BalanceDaoSpec extends AnyFunSuite with PostgresSetup:

  val balanceDao: BalanceDao[ConnectionIO] = BalanceDao.create

  test("BalanceDao") {
    transactor.use { tx =>
      for
        _ <- DB.runMigrations(tx)
        balance = Balance(telegramId = ChatIntId(123), timeLeft = FiniteDuration(60, TimeUnit.SECONDS))
        affectedRows <- balanceDao
          .upsert(balance)
          .transact(tx)
        _ = assert(affectedRows === 1)

        balanceOpt <- balanceDao
          .findBy(telegramId = 123)
          .transact(tx)
        _ = assert(balanceOpt === Some(balance))

        affectedRows <- balanceDao
          .upsert(balance)
          .transact(tx)
        _ = assert(affectedRows === 1)

        affectedRows <- balanceDao
          .upsert(balance)
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

