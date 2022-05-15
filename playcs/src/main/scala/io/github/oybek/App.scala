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
import io.github.oybek.common.PoolManager
import io.github.oybek.common.With
import io.github.oybek.common.logger.ContextData
import io.github.oybek.common.logger.ContextLogger
import io.github.oybek.cstrike.model.Command
import io.github.oybek.database.DB
import io.github.oybek.database.admin.dao.AdminDao
import io.github.oybek.hlds.HldsClient
import io.github.oybek.tg.Tg
import io.github.oybek.hlds.HldsConsole
import io.github.oybek.hub.Hub
import io.github.oybek.password.PasswordGenerator
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
      _ <- resources(config).use {
        (httpClient, consoles, tx) =>
          assembleAndLaunch(config, httpClient, consoles, tx)
      }
    yield ExitCode.Success
  private val log = Slf4jLogger.getLoggerFromName[IO]("application")

def assembleAndLaunch(config: AppConfig,
                      httpClient: Client[IO],
                      consoles: List[HldsConsole[IO]],
                      tx: HikariTransactor[IO]): IO[Unit] =
  val client = Logger(logHeaders = false, logBody = false)(httpClient)
  val api = BotApi[IO](client, s"https://api.telegram.org/bot${config.tgBotApiToken}")
  val passwordGenerator = PasswordGenerator.create[IO]
  val transactor = new FunctionK[ConnectionIO, IO]:
    override def apply[A](a: ConnectionIO[A]): IO[A] =
      a.transact(tx)
  val consolePool = (consoles, Nil)
  val adminDao = AdminDao.create
  for
    contextLogger <- ContextLogger.create[IO]
    given ContextLogger[IO] = contextLogger

    _ <- DB.runMigrations[IO](tx)
    consolePoolRef <- Ref.of[IO, (List[HldsConsole[IO]], List[HldsConsole[IO] With ChatIntId])](consolePool)
    consolePoolManager = PoolManager.create[IO, HldsConsole[IO], ChatIntId](
      consolePoolRef,
      hldsConsole =>
        for
          pass <- passwordGenerator.generate
          _ <- hldsConsole.svPassword(pass)
          _ <- hldsConsole.map("de_dust2")
        yield ()
    )
    hub = Hub.create[IO, ConnectionIO](
      consolePoolManager,
      passwordGenerator,
      transactor)
    tg= Tg.create(api, hub)
    _ <- setCommands(api)
    _ <- tg.start()
  yield ()

def resources(config: AppConfig): Resource[IO, (Client[IO], List[HldsConsole[IO]], HikariTransactor[IO])] =
  val initialPort = 27015
  val telegramResponseWaitTime = 60L
  for
    connEc <- ExecutionContexts.fixedThreadPool[IO](10)
    tranEc <- ExecutionContexts.cachedThreadPool[IO]
    client <- BlazeClientBuilder[IO].withExecutionContext(connEc)
      .withResponseHeaderTimeout(FiniteDuration(telegramResponseWaitTime, TimeUnit.SECONDS))
      .resource
    transactor <- DB.createTransactor[IO](config.database, tranEc)
    consoles <- (0 until config.serverPoolSize)
      .toList
      .traverse { offset =>
        val port = initialPort + offset
        HldsClient.create(port, new File(config.hldsDir)).map {
          HldsConsole.create[IO](config.serverIp, port, _)
        }
      }
  yield (client, consoles, transactor)

def setCommands[F[_] : Functor](api: BotApi[F]): F[Unit] =
  val commands = Command.visible.map(x => BotCommand(x.command, x.description))
  Methods.setMyCommands(commands).exec(api).void
