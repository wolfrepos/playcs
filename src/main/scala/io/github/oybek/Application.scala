package io.github.oybek

import cats.arrow.FunctionK
import cats.effect._
import cats.effect.concurrent.Ref
import cats.implicits._
import doobie.{ConnectionIO, Transactor}
import doobie.implicits.toConnectionIOOps
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.github.oybek.common.Scheduler.toActionOps
import io.github.oybek.config.Config
import io.github.oybek.cstrike.model.Command
import io.github.oybek.cstrike.telegram.ToBotCommandTransformer.commandToBotCommand
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
            case (httpClient, consoles) =>
              assembleAndLaunch(config, httpClient, consoles, null)
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

  private def resources[F[_]: Timer: ConcurrentEffect]
                       (config: Config): Resource[F, (Client[F], List[HldsConsole[F]])] =
    for {
      client <- BlazeClientBuilder[F](global)
        .withResponseHeaderTimeout(FiniteDuration(telegramResponseWaitTime, TimeUnit.SECONDS))
        .resource
      consoles <- (0 until config.serverPoolSize)
        .toList
        .traverse { offset =>
          val port = initialPort + offset
          HLDSConsoleClient.create(port, new File(config.hldsDir)).map {
            new HldsConsoleImpl[F](config.serverIp, port, _)
          }
        }
    } yield (client, consoles)

  private def setCommands(api: BotApi[F]): F[Unit] = {
    val commands = Command.all.map(_.transformInto[BotCommand])
    Methods.setMyCommands(commands).exec(api).void
  }

  private val initialPort = 27015
  private val telegramResponseWaitTime = 60L
  private val log = Slf4jLogger.getLoggerFromName[F]("application")
}
