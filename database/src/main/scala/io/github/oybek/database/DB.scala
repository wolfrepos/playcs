package io.github.oybek.database

import doobie._
import doobie.implicits._
import io.github.oybek.dbrush.model.Migration
import io.github.oybek.dbrush.syntax._
import cats.effect.{Async, BracketThrow, ContextShift}
import doobie.util.transactor.Transactor

object DB {

  def createTransactor[F[_]: Async: ContextShift](driver: String,
                                                  url: String,
                                                  user: String,
                                                  pass: String): Transactor[F] =
    Transactor.fromDriverManager[F](
      driver = driver,
      url = url,
      user = user,
      pass = pass
    )

  def runMigrations[F[_]: BracketThrow](transactor: Transactor[F]): F[Unit] =
    List(
      Migration("create payment table", createPaymentTable)
    ).exec[F](transactor)

  private lazy val createPaymentTable =
    sql"""
         |create table payment (
         |  id bigserial primary key,
         |  rubles bigint not null,
         |  telegram_id bigint not null,
         |  charge_time timestamp with time zone not null
         |)
         |""".stripMargin
}
