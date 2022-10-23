package io.github.oybek.playcs

import cats.implicits.*
import ciris.*

import scala.concurrent.duration.DurationInt
import scala.concurrent.duration.FiniteDuration

case class Config(
    serverIp: String,
    tgBotApiToken: String,
    hldsDir: String,
    serverPoolSize: Int,
    hldsTimeout: FiniteDuration
)

object Config:
  def create[F[_]]: ConfigValue[F, Config] = (
    prop("server.ip").as[String],
    prop("tg.token").as[String],
    prop("hlds.dir").as[String],
    prop("pool.size").as[Int],
    prop("hlds.timeout").as[Int].map(_.minutes)
  ).mapN(Config.apply)
