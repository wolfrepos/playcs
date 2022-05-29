package io.github.oybek.database

import cats.implicits.*
import ciris.*

case class DbConfig(driver: String, url: String, user: String, pass: String)

object DbConfig:
  def load[F[_]]: ConfigValue[F, DbConfig] = (
    prop("database.driver").as[String],
    prop("database.url").as[String],
    prop("database.user").as[String],
    prop("database.pass").as[String]
  ).mapN(DbConfig.apply)
