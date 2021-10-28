package io.github.oybek.config

import pureconfig.ConfigReader.Result
import pureconfig._
import pureconfig.generic.auto._

case class Config(serverIp: String,
                  tgBotApiToken: String,
                  hldsDir: String,
                  serverPoolSize: Int)

object Config {
  def load: Result[Config] =
    ConfigSource.default.load[Config]
}
