package io.github.oybek.playcs

import cats.implicits.*
import ciris.*

case class AppConfig(
    serverIp: String,
    tgBotApiToken: String,
    hldsDir: String,
    serverPoolSize: Int
)

object AppConfig:
  def create[F[_]]: ConfigValue[F, AppConfig] = (
    prop("server.ip").as[String],
    prop("tg.token").as[String],
    prop("hlds.dir").as[String],
    prop("pool.size").as[Int]
  ).mapN(AppConfig.apply)
