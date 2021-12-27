package io.github.oybek.database

import cats.effect.{Async, Blocker, ContextShift, Resource, Sync}
import doobie.hikari.HikariTransactor
import doobie.util.transactor.Transactor
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.github.oybek.database.Migrations.{createBalanceTable, createPaymentTable}
import io.github.oybek.database.config.DbConfig
import io.github.oybek.dbrush.model.Migration
import io.github.oybek.dbrush.syntax._

import scala.concurrent.ExecutionContext

object DB {

  def createTransactor[F[_]: Async: ContextShift](config: DbConfig,
                                                  ec: ExecutionContext,
                                                  blocker: Blocker): Resource[F, HikariTransactor[F]] =
    HikariTransactor.newHikariTransactor[F](
      driverClassName = config.driver,
      url = config.url,
      user = config.user,
      pass = config.pass,
      connectEC = ec,
      blocker = blocker
    )

  def runMigrations[F[_]: Sync](transactor: Transactor[F]): F[Unit] = {
    val log = Slf4jLogger.getLoggerFromName[F]("dbrush")
    List(
      Migration("create payment table", createPaymentTable),
      Migration("create balance table", createBalanceTable)
    ).exec[F](transactor, Option(log.info(_)))
  }
}
