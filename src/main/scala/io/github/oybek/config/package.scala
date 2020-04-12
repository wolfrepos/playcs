package io.github.oybek

import java.io.File

import cats.syntax.all._
import cats.effect.Sync
import com.typesafe.config.ConfigFactory
import pureconfig.error.ConfigReaderException

package object config {

  case class Config(tgBotApiToken: String, hldsDir: String)

  object Config {
    import pureconfig.generic.auto._
    import pureconfig._

    def load[F[_]: Sync](configFileName: Option[String]): F[Config] = {
      Sync[F]
        .delay {
          configFileName
            .map(x => loadConfig[Config](ConfigFactory.parseFile(new File(x))))
            .getOrElse(loadConfig[Config](ConfigFactory.load("application.conf")))
        }
        .flatMap {
          case Left(e) =>
            Sync[F].raiseError[Config](new ConfigReaderException[Config](e))
          case Right(config) =>
            Sync[F].pure(config)
        }
    }
  }
}
