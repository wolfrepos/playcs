package io.github.oybek.database.admin.dao

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.dimafeng.testcontainers.ForAllTestContainer
import com.dimafeng.testcontainers.PostgreSQLContainer
import doobie.*
import doobie.implicits.*
import io.github.oybek.database.DB
import io.github.oybek.database.DbConfig
import io.github.oybek.database.PostgresSetup
import io.github.oybek.database.admin.dao.AdminDao
import org.scalatest.funsuite.AnyFunSuite
import org.testcontainers.utility.DockerImageName
import telegramium.bots.ChatIntId

import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext.global as globalEc
import scala.concurrent.duration.DurationInt
import scala.concurrent.duration.FiniteDuration

trait AdminDaoSpec extends AnyFunSuite with PostgresSetup:

  val adminDao: AdminDao[ConnectionIO] = AdminDao.create

  test("AdminDao") {
    transactor.use { tx =>
      for
        _ <- DB.runMigrations(tx)
        _ <- sql"insert into admin (chat_id) values (123)".update.run.transact(tx)
        isAdmin <- adminDao.isAdmin(123L).transact(tx)
        _ = assert(isAdmin)
        isAdmin <- adminDao.isAdmin(124L).transact(tx)
        _ = assert(!isAdmin)
      yield ()
    }.unsafeRunTimed(10.seconds)
  }

