package io.github.oybek.database

import doobie.Fragment
import doobie.implicits._

object Migrations {

  lazy val createPaymentTable: Fragment =
    sql"""
         |create table payment (
         |  id bigserial primary key,
         |  rubles bigint not null,
         |  telegram_id bigint not null,
         |  charge_time timestamp with time zone not null
         |)
         |""".stripMargin

  lazy val createBalanceTable: Fragment =
    sql"""
         |create table balance (
         |  telegram_id bigint not null,
         |  seconds bigint not null
         |)
         |""".stripMargin
}
