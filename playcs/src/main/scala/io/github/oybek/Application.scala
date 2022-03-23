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
import io.github.oybek.common.PoolManagerImpl
import io.github.oybek.common.Scheduler.every
import io.github.oybek.common.With
import io.github.oybek.common.logger.ContextData
import io.github.oybek.common.logger.ContextLogger
import io.github.oybek.common.time.Timer
import io.github.oybek.common.time.{Clock as Clockk}
import io.github.oybek.config.Config
import io.github.oybek.cstrike.model.Command
import io.github.oybek.database.DB
import io.github.oybek.database.dao.impl.AdminDaoImpl
import io.github.oybek.database.dao.impl.BalanceDaoImpl
import io.github.oybek.integration.HLDSConsoleClient
import io.github.oybek.integration.TGGate
import io.github.oybek.service.HldsConsole
import io.github.oybek.service.impl.ConsoleImpl
import io.github.oybek.service.impl.HldsConsoleImpl
import io.github.oybek.service.impl.PasswordGeneratorImpl
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

given timer: Timer[IO] = (duration: FiniteDuration) => IO.sleep(duration)
given clock: Clockk[IO] = new Clockk[IO] {
  def instantNow: IO[Instant] = IO.realTimeInstant
}

object Application extends IOApp:
  def run(args: List[String]): IO[ExitCode] =
    for
      config <- Config.load[IO].load[IO]
      _ <- log.info(s"loaded config: $config")
      _ <- resources[IO](config).use(
        (httpClient, consoles, tx) =>
          assembleAndLaunch(config, httpClient, consoles, tx)
      )
    yield ExitCode.Success
  private val log = Slf4jLogger.getLoggerFromName[IO]("application")
end Application

def assembleAndLaunch(config: Config,
                      httpClient: Client[IO],
                      consoles: List[HldsConsole[IO]],
                      tx: HikariTransactor[IO]): IO[Unit] =
  val client = Logger(logHeaders = false, logBody = false)(httpClient)
  val api = BotApi[IO](client, s"https://api.telegram.org/bot${config.tgBotApiToken}")
  val passwordGenerator = new PasswordGeneratorImpl[IO]
  val transactor = new FunctionK[ConnectionIO, IO]:
    override def apply[A](a: ConnectionIO[A]): IO[A] =
      a.transact(tx)
  val consolePool = (consoles, Nil)
  for
    consolePoolLogger <- ContextLogger.create[IO]("console-pool-manager")
    consoleLogger <- ContextLogger.create[IO]("console")
    tgGateLogger <- ContextLogger.create[IO]("tg-gate")
    _ <- DB.runMigrations[IO](tx)
    consolePoolRef <- Ref.of[IO, (List[HldsConsole[IO]], List[HldsConsole[IO] With ChatIntId])](consolePool)
    consolePoolManager = new PoolManagerImpl[IO, HldsConsole[IO], ChatIntId](
      consolePoolRef,
      hldsConsole =>
        for
          pass <- passwordGenerator.generate
          _ <- hldsConsole.svPassword(pass)
          _ <- hldsConsole.map("de_dust2")
        yield ()
    )
    console = new ConsoleImpl[IO, ConnectionIO](
      consolePoolManager,
      passwordGenerator,
      AdminDaoImpl,
      transactor,
      consoleLogger)
    tgGate = new TGGate(api, console, tgGateLogger)
    _ <- setCommands(api)
    _ <- tgGate.start()
  yield ()

def resources[F[_] : Timer : Async](config: Config): Resource[F, (Client[F], List[HldsConsole[F]], HikariTransactor[F])] =
  val initialPort = 27015
  val telegramResponseWaitTime = 60L
  for
    connEc <- ExecutionContexts.fixedThreadPool[F](10)
    tranEc <- ExecutionContexts.cachedThreadPool[F]
    client <- BlazeClientBuilder[F].withExecutionContext(connEc)
      .withResponseHeaderTimeout(FiniteDuration(telegramResponseWaitTime, TimeUnit.SECONDS))
      .resource
    transactor <- DB.createTransactor(config.database, tranEc)
    consoles <- (0 until config.serverPoolSize)
      .toList
      .traverse { offset =>
        val port = initialPort + offset
        HLDSConsoleClient.create(port, new File(config.hldsDir)).map {
          new HldsConsoleImpl[F](config.serverIp, port, _)
        }
      }
  yield (client, consoles, transactor)

def setCommands[F[_] : Functor](api: BotApi[F]): F[Unit] =
  val commands = Command.all.map(x => BotCommand(x.command, x.description))
  Methods.setMyCommands(commands).exec(api).void
