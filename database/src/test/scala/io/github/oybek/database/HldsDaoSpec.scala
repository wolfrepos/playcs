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
import org.testcontainers.utility.DockerImageName
import telegramium.bots.ChatIntId

import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext.global as globalEc
import scala.concurrent.duration.FiniteDuration

import doobie.scalatest.IOChecker
import concurrent.duration.DurationInt
import org.scalatest.funsuite.AnyFunSuite
import io.github.oybek.database.hlds.dao.HldsDao
import io.github.oybek.database.hlds.model.Hlds

trait HldsDaoSpec extends AnyFunSuite with DoobieSuite with IOChecker:

  val hldsDao: HldsDao[ConnectionIO] = HldsDao.create
  val hlds: Hlds = Hlds(123L, "0918", "de_dust")

  test("HldsDao.addQuery")     { check(HldsDao.addQuery(hlds)) }
  test("HldsDao.deleteQuery")  { check(HldsDao.deleteQuery(123L)) }
  test("HldsDao.allQuery")     { check(HldsDao.allQuery) }

  test("HldsDao") {
    (WeakAsync.liftK[IO, ConnectionIO]).use { fk =>
        for
          _ <- hldsDao.add(Hlds(123, "0983", "de_dust")).transact(transactor)

          query = for
            _ <- hldsDao.add(Hlds(124, "0983", "de_dust"))
            _ <- fk(IO.raiseError(new Exception("transaction failed")))
          yield ()
          _ <- query.transact(transactor).attempt

          hldss <- hldsDao.all.transact(transactor)
          _ = assert(hldss === List(Hlds(123, "0983", "de_dust")))
        yield ()
    }.unsafeRunSync()
  }

