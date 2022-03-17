package io.github.oybek

import cats.arrow.FunctionK
import cats.effect.*
import cats.implicits.*
import cats.{Functor, Parallel}
import doobie.hikari.HikariTransactor
import doobie.implicits.toConnectionIOOps
import doobie.{ConnectionIO, ExecutionContexts}
import io.github.oybek.common.Scheduler.every
import io.github.oybek.common.logger.{ContextData, ContextLogger}
import io.github.oybek.common.time.{Timer, Clock as Clockk}
import io.github.oybek.config.Config
import io.github.oybek.cstrike.model.Command
import io.github.oybek.database.DB
import io.github.oybek.database.dao.impl.{AdminDaoImpl, BalanceDaoImpl}
import io.github.oybek.integration.{HLDSConsoleClient, TGGate}
import io.github.oybek.model.ConsolePool
import io.github.oybek.service.HldsConsole
import io.github.oybek.service.impl.{ConsoleImpl, HldsConsoleImpl, HldsConsolePoolManagerImpl, PasswordGeneratorImpl}
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.client.Client
import org.http4s.client.middleware.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import telegramium.bots.BotCommand
import telegramium.bots.high.implicits.methodOps
import telegramium.bots.high.{BotApi, Methods}

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
  val consolePool = ConsolePool[IO](free = consoles, busy = Nil)
  val passwordGen = new PasswordGeneratorImpl[IO]
  val transactor = new FunctionK[ConnectionIO, IO] {
    override def apply[A](a: ConnectionIO[A]): IO[A] =
      a.transact(tx)
  }
  for
    consolePoolLogger <- ContextLogger.create[IO]("console-pool-manager")
    consoleLogger <- ContextLogger.create[IO]("console")
    tgGateLogger <- ContextLogger.create[IO]("tg-gate")
    _ <- DB.runMigrations[IO](tx)
    consolePoolRef <- Ref.of[IO, ConsolePool[IO]](consolePool)
    consolePoolManager = new HldsConsolePoolManagerImpl[IO, ConnectionIO](consolePoolRef, passwordGen, consolePoolLogger)
    console = new ConsoleImpl[IO, ConnectionIO](consolePoolManager, BalanceDaoImpl, AdminDaoImpl, transactor, consoleLogger)
    _ <- Spawn[IO].start {
      given ContextData(1234)
      console.expireCheck.every(1.minute)
    }
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
