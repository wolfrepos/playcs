package io.github.oybek

import cats.syntax.all._
import cats.effect.Sync
import com.typesafe.config.ConfigFactory
import pureconfig.error.ConfigReaderException

package object config {

  case class Config(serverIp: String,
                    tgBotApiToken: String,
                    hldsDir: String,
                    serverPoolSize: Int)

  object Config {
    import pureconfig.generic.auto._
    import pureconfig._

    def load[F[_]: Sync]: F[Config] = {
      Sync[F].delay {
        loadConfig[Config](ConfigFactory.load("application.conf"))
      }.flatMap {
        case Left(e) =>
          Sync[F].raiseError[Config](new ConfigReaderException[Config](e))
        case Right(config) =>
          Sync[F].pure(config)
      }
    }
  }
}
