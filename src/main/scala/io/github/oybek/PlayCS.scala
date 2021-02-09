package io.github.oybek

import cats.effect._
import cats.effect.concurrent.Ref
import io.github.oybek.component.pool.CsServerPool
import io.github.oybek.component.telegram.Telegram
import io.github.oybek.config.Config
import io.github.oybek.domain.Server
import java.util.concurrent.TimeUnit
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.client.middleware.Logger
import org.slf4j.LoggerFactory
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import telegramium.bots.high.BotApi

object PlayCS extends IOApp {
  type F[+T] = IO[T]

  private val log = LoggerFactory.getLogger("main")

  def run(args: List[String]): IO[ExitCode] =
    resources.use(httpClient =>
      for {
        config     <- Config.load[F](Option(System.getProperty("application.conf")))
        _          <- Sync[F].delay { log.info(s"loaded config: $config") }
        poolRef    <- Ref[F].of(List.empty[Server[F]])
        serverPool = new CsServerPool[F](poolRef, config)
        _          <- serverPool.init
        client     = Logger(logHeaders = false, logBody = false)(httpClient)
        tgBotApi   = BotApi[F](client, s"https://api.telegram.org/bot${config.tgBotApiToken}")
        telegram   = new Telegram(tgBotApi, serverPool)
        _          <- telegram.start
      } yield ()
    ).as(ExitCode.Success)

  private def resources: Resource[F, Client[F]] =
    BlazeClientBuilder[F](global)
      .withResponseHeaderTimeout(FiniteDuration(60, TimeUnit.SECONDS))
      .resource
}
