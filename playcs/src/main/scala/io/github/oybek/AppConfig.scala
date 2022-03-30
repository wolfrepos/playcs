package io.github.oybek

import cats.implicits.*
import ciris.*
import io.github.oybek.database.DbConfig

case class AppConfig(database: DbConfig,
                     serverIp: String,
                     tgBotApiToken: String,
                     hldsDir: String,
                     serverPoolSize: Int)

object AppConfig:
  def create[F[_]]: ConfigValue[F, AppConfig] = (
    DbConfig.load,
    prop("server.ip").as[String],
    prop("tg.token").as[String],
    prop("hlds.dir").as[String],
    prop("pool.size").as[Int]
  ).mapN(AppConfig.apply)
