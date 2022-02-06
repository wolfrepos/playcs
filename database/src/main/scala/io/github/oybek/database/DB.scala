package io.github.oybek.database

import cats.effect.{Async, Resource, Sync}
import doobie.hikari.HikariTransactor
import doobie.util.transactor.Transactor
import io.github.oybek.database.config.DbConfig
import org.flywaydb.core.Flyway
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.concurrent.ExecutionContext

object DB:
  def createTransactor[F[_]: Async](config: DbConfig,
                                    ec: ExecutionContext): Resource[F, HikariTransactor[F]] =
    HikariTransactor.newHikariTransactor[F](
      driverClassName = config.driver,
      url = config.url,
      user = config.user,
      pass = config.pass,
      connectEC = ec,
    )

  def runMigrations[F[_]: Sync](transactor: HikariTransactor[F]): F[Unit] =
    transactor.configure {
      dataSource =>
        Sync[F].delay {
          Flyway
            .configure()
            .dataSource(dataSource)
            .load()
            .migrate()
        }
    }
