package io.github.oybek.database

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.dimafeng.testcontainers.{ForAllTestContainer, PostgreSQLContainer}
import doobie.*
import doobie.implicits.*
import io.github.oybek.database.config.DbConfig
import io.github.oybek.database.dao.impl.{AdminDaoImpl, BalanceDaoImpl}
import io.github.oybek.database.dao.{AdminDao, BalanceDao}
import io.github.oybek.database.model.Balance
import org.scalatest.flatspec.AnyFlatSpec
import org.testcontainers.utility.DockerImageName
import telegramium.bots.ChatIntId

import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext.global as globalEc
import scala.concurrent.duration.{DurationInt, FiniteDuration}

class AdminDaoSpec extends AnyFlatSpec with PostgresSetup:

  val adminDao: AdminDao[ConnectionIO] = AdminDaoImpl

  "isAdmin".should("work") in {
    transactor.use { tx =>
      for
        _ <- DB.runMigrations(tx)
        _ <- sql"insert into admin (telegram_id) values (123)".update.run.transact(tx)
        isAdmin <- adminDao.isAdmin(123L).transact(tx)
        _ = assert(isAdmin)
        isAdmin <- adminDao.isAdmin(124L).transact(tx)
        _ = assert(!isAdmin)
      yield ()
    }.unsafeRunTimed(10.seconds)
  }

