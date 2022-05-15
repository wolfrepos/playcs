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

import concurrent.duration.DurationInt
import org.scalatest.funsuite.AnyFunSuite
import io.github.oybek.database.hlds.dao.HldsDao
import io.github.oybek.database.hlds.model.Hlds

trait TransactionSpec extends AnyFunSuite with PostgresSetup with doobie.free.Instances with doobie.syntax.AllSyntax:
  val someDao: HldsDao[ConnectionIO] = ???

  test("TransactionSpec") {
    (transactor, WeakAsync.liftK[IO, ConnectionIO]).mapN((x, y) => (x, y)).use {
      (tx, fk) =>
        for
          _ <- DB.runMigrations(tx)
          _ <- someDao.add(Hlds(123, "0983", "de_dust")).transact(tx)

          query = for
            _ <- someDao.add(Hlds(124, "0983", "de_dust"))
            _ <- fk(IO.raiseError(new Exception("transaction failed")))
          yield ()
          _ <- query.transact(tx).attempt

          hldss <- someDao.all.transact(tx)
          _ = assert(hldss === List(Hlds(123, "0983", "de_dust")))
        yield ()
    }.unsafeRunSync()
  }

