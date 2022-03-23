package io.github.oybek.config

import cats.implicits.*
import ciris.*
import io.github.oybek.database.config.DbConfig

case class Config(database: DbConfig,
                  serverIp: String,
                  tgBotApiToken: String,
                  hldsDir: String,
                  serverPoolSize: Int)

object Config:
  def create[F[_]]: ConfigValue[F, Config] = (
    DbConfig.load,
    prop("server.ip").as[String],
    prop("tg.token").as[String],
    prop("hlds.dir").as[String],
    prop("pool.size").as[Int]
  ).mapN(Config.apply)
