package io.github.oybek

import cats.instances.option._
import cats.syntax.all._
import cats.effect._
import io.github.oybek.domain.CmdStartCSDS
import io.github.oybek.service.Octopus

import scala.concurrent.duration._
import java.io.File
import java.util.concurrent.TimeUnit

import cats.effect.concurrent.Ref
import io.github.oybek.config.Config
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.client.middleware.Logger
import org.slf4j.LoggerFactory
import telegramium.bots.client.{Api, ApiHttp4sImp}

import scala.concurrent.ExecutionContext.Implicits.global

object PlayCS extends IOApp {

  type F[+T] = IO[T]

  private val tgBotApiToken = "1215497177:AAFmxBAXCG71dE2eIh22YoMKDQ3eSoiaPg8"
  private val log = LoggerFactory.getLogger("Main")

  def run(args: List[String]): IO[ExitCode] =
    for {
      ref <- Ref[F].of(Option.empty[Octopus[F]])
      configFile <- Sync[F].delay(Option(System.getProperty("application.conf")))
      config <- Config.load[F](configFile)
      _ <- Sync[F].delay { log.info(s"loaded config: $config") }
      _ <- Sync[F].delay { log.info(s"starting service...") }
      _ <- resources
        .use { httpClient =>
          implicit val client   : Client[F] = Logger(logHeaders = false, logBody = false)(httpClient)
          implicit val tgBotApi : Api[F]    = new ApiHttp4sImp[F](client, s"https://api.telegram.org/bot${config.tgBotApiToken}")
          val tgBot = new TgBot[F](config, ref)
          tgBot.start
        }
    } yield ExitCode.Success

  private def resources: Resource[F, Client[F]] =
    BlazeClientBuilder[F](global)
      .withResponseHeaderTimeout(FiniteDuration(60, TimeUnit.SECONDS))
      .resource
}

