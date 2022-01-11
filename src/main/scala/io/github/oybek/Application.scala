package io.github.oybek

import cats.arrow.FunctionK
import cats.effect._
import cats.implicits._
import cats.{Functor, Parallel}
import doobie.hikari.HikariTransactor
import doobie.implicits.toConnectionIOOps
import doobie.{ConnectionIO, ExecutionContexts}
import io.github.oybek.common.Scheduler.toActionOps
import io.github.oybek.common.time.{Timer, Clock => Clockk}
import io.github.oybek.config.Config
import io.github.oybek.cstrike.model.Command
import io.github.oybek.database.DB
import io.github.oybek.database.dao.impl.BalanceDaoImpl
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
import scala.concurrent.duration._

object Application extends IOApp {
  def run(args: List[String]): IO[ExitCode] =
    Config.load match {
      case Right(config) =>
        implicit val timer: Timer[IO] = (duration: FiniteDuration) => Temporal[IO].sleep(duration)
        for {
          _ <- log.info(s"loaded config: $config")
          _ <- resources[IO](config).use {
            case (httpClient, consoles, tx) =>
              assembleAndLaunch(config, httpClient, consoles, tx)
          }
        } yield ExitCode.Success

      case Left(err) =>
        log.error(s"Could not load config file $err").as(ExitCode.Error)
    }

  private def assembleAndLaunch[F[_]: Parallel: Temporal: Async: Spawn](config: Config,
                                                                        httpClient: Client[F],
                                                                        consoles: List[HldsConsole[F]],
                                                                        tx: HikariTransactor[F]): F[Unit] = {
    val client      = Logger(logHeaders = false, logBody = false)(httpClient)
    val api         = BotApi[F](client, s"https://api.telegram.org/bot${config.tgBotApiToken}")
    val log         = Slf4jLogger.getLoggerFromName[F]("console-pool-manager")
    val consoleLog  = Slf4jLogger.getLoggerFromName[F]("console")
    val consolePool = ConsolePool[F](free = consoles, busy = Nil)
    val passwordGen = new PasswordGeneratorImpl[F]
    val transactor  = new FunctionK[ConnectionIO, F] {
      override def apply[A](a: ConnectionIO[A]): F[A] =
        a.transact(tx)
    }
    implicit val clock: Clockk[F] = new Clockk[F] {
      def instantNow: F[Instant] = Temporal[F].realTimeInstant
    }

    for {
      _                  <- DB.runMigrations[F](tx)
      consolePoolRef     <- Ref.of[F, ConsolePool[F]](consolePool)
      consolePoolManager  = new HldsConsolePoolManagerImpl[F, ConnectionIO](
        consolePoolRef, passwordGen, BalanceDaoImpl, transactor, log)
      _                  <- Spawn[F].start(consolePoolManager.expireCheck.every(1.minute))
      console             = new ConsoleImpl(consolePoolManager, BalanceDaoImpl, transactor, consoleLog)
      tgGate              = new TGGate(api, console)
      _                  <- setCommands(api)
      _                  <- tgGate.start()
    } yield ()
  }

  private def resources[F[_]: Timer: Async]
                       (config: Config): Resource[F, (Client[F], List[HldsConsole[F]], HikariTransactor[F])] =
    for {
      connEc <- ExecutionContexts.fixedThreadPool[F](10)
      tranEc <- ExecutionContexts.cachedThreadPool[F]
      client <- BlazeClientBuilder[F](connEc)
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
    } yield (client, consoles, transactor)

  private def setCommands[F[_]: Functor](api: BotApi[F]): F[Unit] = {
    val commands = Command.all.map(x => BotCommand(x.command, x.description))
    Methods.setMyCommands(commands).exec(api).void
  }

  private val initialPort = 27015
  private val telegramResponseWaitTime = 60L
  private val log = Slf4jLogger.getLoggerFromName[IO]("application")
}
