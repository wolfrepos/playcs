package io.github.oybek

import cats.effect._
import io.github.oybek.common.TimeTools.PF
import io.github.oybek.config.Config
import io.github.oybek.cstrike.service.Translator
import io.github.oybek.cstrike.service.impl.TranslatorImpl
import io.github.oybek.playcs.bot.Bot
import io.github.oybek.playcs.service.Manager
import io.github.oybek.playcs.service.impl.ManagerImpl
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.client.middleware.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import telegramium.bots.high.BotApi

import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object Application extends IOApp {
  type F[+T] = IO[T]

  def run(args: List[String]): IO[ExitCode] = resources.use { httpClient =>
    for {
      config  <- Config.load[F]
      _ <- log.info(s"loaded config: $config")
      client = Logger(logHeaders = false, logBody = false)(httpClient)
      api = BotApi[F](client, s"https://api.telegram.org/bot${config.tgBotApiToken}")
      manager <- Manager.create[F](config)
      _ <- manager.expireCheck.every(2.minutes).start
      translator = Translator.create
      bot = new Bot(api, manager, translator)
      _ <- bot.start()
    } yield ()
  }.as(ExitCode.Success)

  private def resources: Resource[F, Client[F]] =
    BlazeClientBuilder[F](global)
      .withResponseHeaderTimeout(FiniteDuration(60, TimeUnit.SECONDS))
      .resource

  private val log = Slf4jLogger.getLoggerFromName[F]("main")
}
