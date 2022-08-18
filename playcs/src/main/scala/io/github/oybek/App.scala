package io.github.oybek

import cats.Functor
import cats.Parallel
import cats.arrow.FunctionK
import cats.effect.*
import cats.implicits.*
import doobie.ConnectionIO
import doobie.ExecutionContexts
import doobie.hikari.HikariTransactor
import doobie.implicits.toConnectionIOOps
import io.github.oybek.common.Pool
import io.github.oybek.common.With
import io.github.oybek.common.logger.ContextData
import io.github.oybek.common.logger.ContextLogger
import io.github.oybek.cstrike.model.Command
import io.github.oybek.hlds.Hlds
import io.github.oybek.hlds.HldsClient
import io.github.oybek.hub.Hub
import io.github.oybek.password.PasswordGenerator
import io.github.oybek.tg.Tg
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
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.*

object App extends IOApp:
  def run(args: List[String]): IO[ExitCode] =
    for
      config <- AppConfig.create[IO].load[IO]
      _ <- log.info(s"loaded config: $config")
      _ <- resources(config).use { (httpClient, consoles) =>
        assembleAndLaunch(config, httpClient, consoles)
      }
    yield ExitCode.Success
  private val log = Slf4jLogger.getLoggerFromName[IO]("application")

def assembleAndLaunch(
    config: AppConfig,
    httpClient: Client[IO],
    consoles: List[Hlds[IO]]
): IO[Unit] =
  val client = Logger(logHeaders = false, logBody = false)(httpClient)
  val api =
    BotApi[IO](client, s"https://api.telegram.org/bot${config.tgBotApiToken}")
  val passwordGenerator = PasswordGenerator.create[IO]
  val consolePool = (consoles, Nil)
  for
    contextLogger <- ContextLogger.create[IO]
    given ContextLogger[IO] = contextLogger
    consolePoolManager <- Pool.create[IO, Long, Hlds[IO]](
      consolePool,
      hldsConsole =>
        for
          pass <- passwordGenerator.generate
          _ <- hldsConsole.svPassword(pass)
          _ <- hldsConsole.map("de_dust2")
        yield ()
    )
    hub = Hub.create[IO, ConnectionIO](
      consolePoolManager,
      passwordGenerator
    )
    tg = Tg.create(api, hub)
    _ <- setCommands(api)
    _ <- tg.start()
  yield ()

def resources(
    config: AppConfig
): Resource[IO, (Client[IO], List[Hlds[IO]])] =
  val initialPort = 27015
  val telegramResponseWaitTime = 60L
  for
    connEc <- ExecutionContexts.fixedThreadPool[IO](10)
    tranEc <- ExecutionContexts.cachedThreadPool[IO]
    client <- BlazeClientBuilder[IO]
      .withExecutionContext(connEc)
      .withResponseHeaderTimeout(
        FiniteDuration(telegramResponseWaitTime, TimeUnit.SECONDS)
      )
      .resource
    consoles <- (0 until config.serverPoolSize).toList
      .traverse { offset =>
        val port = initialPort + offset
        HldsClient.create(port, new File(config.hldsDir)).map {
          Hlds.create[IO](config.serverIp, port, _)
        }
      }
  yield (client, consoles)

def setCommands[F[_]: Functor](api: BotApi[F]): F[Unit] =
  val commands = Command.visible.map(x => BotCommand(x.command, x.description))
  Methods.setMyCommands(commands).exec(api).void
