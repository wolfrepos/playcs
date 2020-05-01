package io.github.oybek

import java.util.concurrent.TimeUnit

import cats.effect._
import cats.effect.concurrent.Ref
import io.github.oybek.config.Config
import io.github.oybek.domain.Server
import io.github.oybek.service.pool.{CsServerPool, ServerPoolAlg}
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.client.middleware.Logger
import org.slf4j.LoggerFactory
import telegramium.bots.client._
import telegramium.bots.high.LongPollBot

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

import cats.syntax.all._
import fs2.Stream

object PlayCS extends IOApp {
  type F[+T] = IO[T]

  private val log = LoggerFactory.getLogger("Main")

  def run(args: List[String]): IO[ExitCode] =
    resources
      .use { httpClient =>
        Stream
          .eval {
            for {
              config <- Config.load[F](
                Option(System.getProperty("application.conf"))
              )
              _ <- Sync[F].delay { log.info(s"loaded config: $config") }
              poolRef <- Ref[F].of(List.empty[Server[F]])
              serverPool = new CsServerPool[F](poolRef, config)
              _ <- serverPool.init
              client = Logger(logHeaders = false, logBody = false)(httpClient)
              tgBotApi = new ApiHttp4sImp[F](
                client,
                s"https://api.telegram.org/bot${config.tgBotApiToken}"
              )
            } yield (serverPool, config, tgBotApi)
          }
          .flatMap {
            case (serverPool, config, tgBotApi) =>
              implicit val botApi: Api[F] = tgBotApi
              new Bot[F](serverPool, config.hldsDir)
                .process(LongPollBot.getUpdates(tgBotApi))
                .collect { case Some(x) => x }
                .evalMap {
                  case x: SendMessageReq        => tgBotApi.sendMessage(x)
                  case x: EditMessageTextReq    => tgBotApi.editMessageText(x)
                  case x: SendPhotoReq          => tgBotApi.sendPhoto(x)
                  case x: EditMessageMediaReq   => tgBotApi.editMessageMedia(x)
                  case x: EditMessageCaptionReq => tgBotApi.editMessageCaption(x)
                }
          }
          .metered(100 millis)
          .compile
          .drain
      }
      .as(ExitCode.Success)

  private def resources: Resource[F, Client[F]] =
    BlazeClientBuilder[F](global)
      .withResponseHeaderTimeout(FiniteDuration(60, TimeUnit.SECONDS))
      .resource
}
