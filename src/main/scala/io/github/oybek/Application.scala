package io.github.oybek

import cats.effect._
import cats.implicits.toTraverseOps
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.github.oybek.common.TimeTools.PF
import io.github.oybek.config.Config
import io.github.oybek.console.service.ConsoleHigh
import io.github.oybek.cstrike.service.Translator
import io.github.oybek.playcs.bot.Bot
import io.github.oybek.playcs.service.Manager
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.client.middleware.Logger
import telegramium.bots.high.BotApi

import java.io.File
import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object Application extends IOApp {
  type F[+T] = IO[T]

  def run(args: List[String]): F[ExitCode] = {
    for {
      config <- Config.load[F]
      _ <- log.info(s"loaded config: $config")
      _ <- resources(config).use {
        case (httpClient, consoles) =>
          val client = Logger(logHeaders = false, logBody = false)(httpClient)
          val api = BotApi[F](client, s"https://api.telegram.org/bot${config.tgBotApiToken}")
          for {
            manager <- Manager.create[F](consoles)
            _ <- manager.expireCheck.every(2.minutes).start
            _ <- new Bot(api, manager, Translator).start()
          } yield ()
      }
    } yield ()
  }.as(ExitCode.Success)

  private def resources[F[_]: Timer: ConcurrentEffect]
                       (config: Config): Resource[F, (Client[F], List[ConsoleHigh[F]])] =
    for {
      client <- BlazeClientBuilder[F](global)
        .withResponseHeaderTimeout(FiniteDuration(telegramResponseWaitTime, TimeUnit.SECONDS))
        .resource
      consoles <- (0 until config.serverPoolSize)
        .toList
        .traverse(i => ConsoleHigh.create(config.serverIp, 27015 + i, new File(config.hldsDir)))
    } yield (client, consoles)

  private val telegramResponseWaitTime = 60L
  private val log = Slf4jLogger.getLoggerFromName[F]("Application")
}
