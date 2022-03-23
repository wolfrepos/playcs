package io.github.oybek.database

import cats.MonadThrow
import cats.effect.IO
import cats.effect.LiftIO
import cats.effect.implicits.*
import cats.effect.unsafe.implicits.global
import cats.implicits.*
import cats.implicits.toTraverseOps
import com.dimafeng.testcontainers.ForAllTestContainer
import com.dimafeng.testcontainers.PostgreSQLContainer
import doobie.*
import doobie.implicits.*
import io.github.oybek.database.config.DbConfig
import io.github.oybek.database.dao.BalanceDao
import io.github.oybek.database.model.Balance
import org.scalatest.flatspec.AnyFlatSpec
import org.testcontainers.utility.DockerImageName
import telegramium.bots.ChatIntId

import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext.global as globalEc
import scala.concurrent.duration.FiniteDuration

import concurrent.duration.DurationInt

class TransactionSpec extends AnyFlatSpec with PostgresSetup with doobie.free.Instances with doobie.syntax.AllSyntax:
  val balanceDao: BalanceDao[ConnectionIO] = BalanceDao.create
  val balance = Balance(
    telegramId = ChatIntId(123),
    timeLeft = FiniteDuration(60, TimeUnit.SECONDS)
  )
  extension (balance: Balance)
    def add(duration: FiniteDuration): Balance =
      balance.copy(timeLeft = balance.timeLeft + duration)

  "transaction".should("fail and rollback") in {
    (transactor, WeakAsync.liftK[IO, ConnectionIO]).mapN((x, y) => (x, y)).use {
      (tx, fk) =>
        for
          _ <- DB.runMigrations(tx)
          _ <- balanceDao.upsert(balance).transact(tx)

          query = for
            _ <- balanceDao.upsert(balance.add(5.seconds))
            balanceOpt <- balanceDao.findBy(balance.telegramId.id)
            _ <- balanceOpt.traverse { b =>
              balanceDao.upsert(balance.add(5.seconds))
            }
            _ <- fk(IO.raiseError(new Exception("transaction failed")))
          yield ()
          _ <- query.transact(tx).attempt

          balanceOpt <- balanceDao.findBy(balance.telegramId.id).transact(tx)
          _ = assert(balanceOpt === Some(balance.copy(timeLeft = 60.seconds)))
        yield ()
    }.unsafeRunSync()
  }

