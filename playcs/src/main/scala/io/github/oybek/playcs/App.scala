package io.github.oybek.playcs

import cats.Functor
import cats.Parallel
import cats.arrow.FunctionK
import cats.effect.*
import cats.implicits.*
import io.github.oybek.playcs.client.HldsClient
import io.github.oybek.playcs.client.HldsDriver
import io.github.oybek.playcs.client.TgClient
import io.github.oybek.playcs.common.Pool
import io.github.oybek.playcs.domain.Command
import io.github.oybek.playcs.service.Bot
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.client.Client
import org.http4s.client.middleware.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import telegramium.bots.BotCommand
import telegramium.bots.ChatIntId
import telegramium.bots.high.BotApi
import telegramium.bots.high.Methods
import telegramium.bots.high.implicits.methodOps

import java.io.File
import java.time.Instant
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.*

object App extends IOApp:
  def run(args: List[String]): IO[ExitCode] =
    for
      config <- Config.create[IO].load[IO]
      _ <- log.info(s"loaded config: $config")
      _ <- resources(config).use { (httpClient, consoles) =>
        assembleAndLaunch(config, httpClient, consoles)
      }
    yield ExitCode.Success
  private val log = Slf4jLogger.getLoggerFromName[IO]("application")

def assembleAndLaunch(config: Config, httpClient: Client[IO], consoles: List[HldsClient]): IO[Unit] =
  val client = Logger(logHeaders = false, logBody = false)(httpClient)
  val api =
    BotApi[IO](client, s"https://api.telegram.org/bot${config.tgBotApiToken}")
  val consolePool = (consoles, Nil)
  for
    consolePoolManager <- Pool.create[IO, HldsClient](
      consolePool,
      hldsConsole =>
        for
          pass <- generatePassword
          _ <- hldsConsole.svPassword(pass)
          _ <- hldsConsole.map("de_dust2")
        yield ()
    )
    bot = Bot.create(
      config.hldsTimeout,
      consolePoolManager
    )
    tgClient = TgClient.create(api, bot)
    _ <- setCommands(api)
    _ <- tgClient.start()
  yield ()

def resources(config: Config): Resource[IO, (Client[IO], List[HldsClient])] =
  val initialPort = 27015
  val telegramResponseWaitTime = 60L
  val connEc = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(10))
  for
    client <- BlazeClientBuilder[IO]
      .withExecutionContext(connEc)
      .withResponseHeaderTimeout(
        FiniteDuration(telegramResponseWaitTime, TimeUnit.SECONDS)
      )
      .resource
    consoles <- (0 until config.serverPoolSize).toList
      .traverse { offset =>
        val port = initialPort + offset
        HldsDriver.create(port, new File(config.hldsDir)).map {
          HldsClient.create(config.serverIp, port, _)
        }
      }
  yield (client, consoles)

def setCommands[F[_]: Functor](api: BotApi[F]): F[Unit] =
  val commands = Command.visible.map(x => BotCommand(x.command, x.description))
  Methods.setMyCommands(commands).exec(api).void

def generatePassword: IO[String] =
  (scala.util.Random.nextInt(9000) + 1000).toString.pure[IO]
