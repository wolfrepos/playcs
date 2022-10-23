package io.github.oybek.playcs

import cats.Functor
import cats.Parallel
import cats.arrow.FunctionK
import cats.effect.*
import cats.implicits.*
import io.github.oybek.playcs.client.HldsClient
import io.github.oybek.playcs.client.HldsClientLow
import io.github.oybek.playcs.client.TgClient
import io.github.oybek.playcs.common.Pool
import io.github.oybek.playcs.common.logger.ContextData
import io.github.oybek.playcs.common.logger.ContextLogger
import io.github.oybek.playcs.cstrike.model.Command
import io.github.oybek.playcs.service.Hub
import io.github.oybek.playcs.service.PasswordService
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

def assembleAndLaunch(
    config: Config,
    httpClient: Client[IO],
    consoles: List[HldsClient[IO]]
): IO[Unit] =
  val client = Logger(logHeaders = false, logBody = false)(httpClient)
  val api =
    BotApi[IO](client, s"https://api.telegram.org/bot${config.tgBotApiToken}")
  val passwordGenerator = PasswordService.create[IO]
  val consolePool = (consoles, Nil)
  for
    contextLogger <- ContextLogger.create[IO]
    given ContextLogger[IO] = contextLogger
    consolePoolManager <- Pool.create[IO, HldsClient[IO]](
      consolePool,
      hldsConsole =>
        for
          pass <- passwordGenerator.generate
          _ <- hldsConsole.svPassword(pass)
          _ <- hldsConsole.map("de_dust2")
        yield ()
    )
    hub = Hub.create[IO](
      config.hldsTimeout,
      consolePoolManager,
      passwordGenerator
    )
    tgClient = TgClient.create(api, hub)
    _ <- setCommands(api)
    _ <- tgClient.start()
  yield ()

def resources(config: Config): Resource[IO, (Client[IO], List[HldsClient[IO]])] =
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
        HldsClientLow.create(port, new File(config.hldsDir)).map {
          HldsClient.create[IO](config.serverIp, port, _)
        }
      }
  yield (client, consoles)

def setCommands[F[_]: Functor](api: BotApi[F]): F[Unit] =
  val commands = Command.visible.map(x => BotCommand(x.command, x.description))
  Methods.setMyCommands(commands).exec(api).void
