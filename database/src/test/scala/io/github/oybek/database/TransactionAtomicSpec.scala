package io.github.oybek.database

import cats.MonadThrow
import cats.effect.IO
import cats.implicits.toTraverseOps
import cats.effect.implicits.*
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
import concurrent.duration.DurationInt
import cats.effect.LiftIO

class TransactionAtomicSpec extends AnyFlatSpec with PostgresSetup with doobie.free.Instances with doobie.syntax.AllSyntax:
  val balanceDao: BalanceDao[ConnectionIO] = BalanceDaoImpl
  val balance = Balance(telegramId = ChatIntId(123), timeLeft = FiniteDuration(60, TimeUnit.SECONDS))

  "checking transaction fail".should("work") in {
    transactor.use { tx =>
      for
        _ <- DB.runMigrations(tx)

        affectedRows <- balanceDao.add(balance).transact(tx)
        query = for
          _ <- balanceDao.update(balance.copy(timeLeft = balance.timeLeft + 5.seconds))
          balanceOpt <- balanceDao.findBy(balance.telegramId.id)
          _ <- IO.println("hi").to[ConnectionIO]
          _ <- balanceOpt.traverse { b =>
            balanceDao.update(balance.copy(timeLeft = b.timeLeft + 5.seconds))
          }
        yield ()
        _ <- query.transact(tx).attempt

        balanceOpt <- balanceDao.findBy(balance.telegramId.id).transact(tx)
        _ = assert(balanceOpt === Some(balance.copy(timeLeft = 70.seconds)))

      yield ()
    }.unsafeRunSync()
  }

