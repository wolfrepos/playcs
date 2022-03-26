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
import io.github.oybek.database.DbConfig
import io.github.oybek.database.balance.dao.BalanceDao
import io.github.oybek.database.balance.model.Balance
import org.testcontainers.utility.DockerImageName
import telegramium.bots.ChatIntId

import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext.global as globalEc
import scala.concurrent.duration.FiniteDuration

import concurrent.duration.DurationInt
import org.scalatest.funsuite.AnyFunSuite

trait TransactionSpec extends AnyFunSuite with PostgresSetup with doobie.free.Instances with doobie.syntax.AllSyntax:
  val someDao: BalanceDao[ConnectionIO] = BalanceDao.create
  val balance = Balance(
    telegramId = ChatIntId(123),
    timeLeft = FiniteDuration(60, TimeUnit.SECONDS)
  )
  extension (balance: Balance)
    def add(duration: FiniteDuration): Balance =
      balance.copy(timeLeft = balance.timeLeft + duration)

  test("TransactionSpec") {
    (transactor, WeakAsync.liftK[IO, ConnectionIO]).mapN((x, y) => (x, y)).use {
      (tx, fk) =>
        for
          _ <- DB.runMigrations(tx)
          _ <- someDao.upsert(balance).transact(tx)

          query = for
            _ <- someDao.upsert(balance.add(5.seconds))
            balanceOpt <- someDao.findBy(balance.telegramId.id)
            _ <- balanceOpt.traverse { b =>
              someDao.upsert(balance.add(5.seconds))
            }
            _ <- fk(IO.raiseError(new Exception("transaction failed")))
          yield ()
          _ <- query.transact(tx).attempt

          balanceOpt <- someDao.findBy(balance.telegramId.id).transact(tx)
          _ = assert(balanceOpt === Some(balance.copy(timeLeft = 60.seconds)))
        yield ()
    }.unsafeRunSync()
  }

