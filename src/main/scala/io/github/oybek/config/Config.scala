package io.github.oybek.config

import cats.syntax.all._
import cats.effect.Sync
import com.typesafe.config.ConfigFactory
import pureconfig.error.ConfigReaderException

case class Config(serverIp: String,
                  tgBotApiToken: String,
                  hldsDir: String,
                  serverPoolSize: Int)

object Config {
  import pureconfig.generic.auto._
  import pureconfig._

  def load[F[_]: Sync]: F[Config] = {
    Sync[F].delay {
      ConfigSource.fromConfig(
        ConfigFactory.load("application.conf")
      ).load[Config]
    }.flatMap {
      case Left(e) =>
        Sync[F].raiseError[Config](new ConfigReaderException[Config](e))
      case Right(config) =>
        Sync[F].pure(config)
    }
  }
}
