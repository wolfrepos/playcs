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
import scala.concurrent.duration.DurationInt
import scala.concurrent.duration.FiniteDuration
import doobie.scalatest.IOChecker
import io.github.oybek.database.DoobieSuite

trait AdminDaoSpec extends AnyFunSuite with IOChecker with DoobieSuite:

  val adminDao: AdminDao[ConnectionIO] = AdminDao.create

  test("AdminDao.isAdminQuery") {
    check(AdminDao.isAdminQuery(123L))
  }

  test("AdminDao") {
    for
      _ <- sql"insert into admin (chat_id) values (123)".update.run.transact(transactor)
      isAdmin <- adminDao.isAdmin(123L).transact(transactor)
      _ = assert(isAdmin)
      isAdmin <- adminDao.isAdmin(124L).transact(transactor)
      _ = assert(!isAdmin)
    yield ()
  }

