package io.github.oybek.config

import ciris._
import io.github.oybek.database.config.DbConfig

case class Config(database: DbConfig,
                  serverIp: String,
                  tgBotApiToken: String,
                  hldsDir: String,
                  serverPoolSize: Int)

object Config {
  def load[F[_]]: ConfigValue[F, Config] = {
    for {
      databaseDriver <- prop("database.driver").as[String]
      databaseUrl <- prop("database.url").as[String]
      databaseUser <- prop("database.user").as[String]
      databasePass <- prop("database.pass").as[String]
      databaseConfig = DbConfig(
        driver = databaseDriver,
        url = databaseUrl,
        user = databaseUser,
        pass = databasePass
      )
      serverIp <- prop("server.ip").as[String]
      tgBotApiToken <- prop("tg.token").as[String]
      hldsDir <- prop("hlds.dir").as[String]
      serverPoolSize <- prop("pool.size").as[Int]
    } yield Config(
      database = databaseConfig,
      serverIp = serverIp,
      tgBotApiToken = tgBotApiToken,
      hldsDir = hldsDir,
      serverPoolSize = serverPoolSize
    )
  }
}
