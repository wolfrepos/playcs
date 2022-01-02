package io.github.oybek

import cats.arrow.FunctionK
import cats.effect._
import cats.effect.concurrent.Ref
import cats.implicits._
import doobie.{ConnectionIO, ExecutionContexts, Transactor}
import doobie.implicits.toConnectionIOOps
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.github.oybek.common.Scheduler.toActionOps
import io.github.oybek.config.Config
import io.github.oybek.cstrike.model.Command
import io.github.oybek.cstrike.telegram.ToBotCommandTransformer.commandToBotCommand
import io.github.oybek.database.DB
import io.github.oybek.database.dao.impl.BalanceDaoImpl
import io.github.oybek.integration.{HLDSConsoleClient, TGGate}
import io.github.oybek.model.ConsolePool
import io.github.oybek.service.HldsConsole
import io.github.oybek.service.impl.{ConsoleImpl, HldsConsoleImpl, HldsConsolePoolManagerImpl, PasswordGeneratorImpl}
import io.scalaland.chimney.dsl.TransformerOps
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.client.middleware.Logger
import telegramium.bots.BotCommand
import telegramium.bots.high.implicits.methodOps
import telegramium.bots.high.{BotApi, Methods}

import java.io.File
import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object Application extends IOApp {
  type F[+T] = IO[T]

  def run(args: List[String]): F[ExitCode] =
    Config.load match {
      case Right(config) =>
        for {
          _ <- log.info(s"loaded config: $config")
          _ <- resources(config).use {
            case (httpClient, consoles, tx) =>
              assembleAndLaunch(config, httpClient, consoles, tx)
          }
        } yield ExitCode.Success

      case Left(_) =>
        log.error("Could not load config file").as(ExitCode.Error)
    }

  private def assembleAndLaunch(config: Config,
                                httpClient: Client[F],
                                consoles: List[HldsConsole[F]],
                                tx: Transactor[F]): IO[Unit] = {
    val client      = Logger(logHeaders = false, logBody = false)(httpClient)
    val api         = BotApi[F](client, s"https://api.telegram.org/bot${config.tgBotApiToken}")
    val log         = Slf4jLogger.getLoggerFromName[F]("console-pool-manager")
    val consolePool = ConsolePool[F](free = consoles, busy = Nil)
    val passwordGen = new PasswordGeneratorImpl[F]
    val transactor  = new FunctionK[ConnectionIO, F] {
      override def apply[A](a: ConnectionIO[A]): F[A] =
        a.transact(tx)
    }
    for {
      consolePoolRef     <- Ref.of[F, ConsolePool[F]](consolePool)
      consolePoolManager  = new HldsConsolePoolManagerImpl[F](consolePoolRef, passwordGen, log)
      _                  <- consolePoolManager.expireCheck.every(1.minute).start
      console             = new ConsoleImpl(consolePoolManager)
      tgGate              = new TGGate(api, console)
      _                  <- setCommands(api)
      _                  <- tgGate.start()
    } yield ()
  }

  private def resources[F[_]: ContextShift: Timer: ConcurrentEffect]
                       (config: Config): Resource[F, (Client[F], List[HldsConsole[F]], Transactor[F])] =
    for {
      blocker <- Blocker[F]
      connEc <- ExecutionContexts.fixedThreadPool[F](10)
      tranEc <- ExecutionContexts.cachedThreadPool[F]
      client <- BlazeClientBuilder[F](connEc)
        .withResponseHeaderTimeout(FiniteDuration(telegramResponseWaitTime, TimeUnit.SECONDS))
        .resource
      transactor <- DB.createTransactor(config.database, tranEc, blocker)
      consoles <- (0 until config.serverPoolSize)
        .toList
        .traverse { offset =>
          val port = initialPort + offset
          HLDSConsoleClient.create(port, new File(config.hldsDir)).map {
            new HldsConsoleImpl[F](config.serverIp, port, _)
          }
        }
    } yield (client, consoles, transactor)

  private def setCommands(api: BotApi[F]): F[Unit] = {
    val commands = Command.all.map(_.transformInto[BotCommand])
    Methods.setMyCommands(commands).exec(api).void
  }

  private val initialPort = 27015
  private val telegramResponseWaitTime = 60L
  private val log = Slf4jLogger.getLoggerFromName[F]("application")
}
