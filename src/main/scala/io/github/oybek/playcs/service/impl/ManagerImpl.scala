package io.github.oybek.playcs.service.impl

import cats.effect.concurrent.Ref
import cats.effect.{Clock, Sync, Timer}
import cats.implicits.{catsSyntaxApplicativeId, catsSyntaxEitherId, toTraverseOps}
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.github.oybek.common.WithMeta
import io.github.oybek.common.WithMeta.toMetaOps
import io.github.oybek.console.service.ConsoleHigh
import io.github.oybek.playcs.model.{ConsoleMeta, ConsolePool}
import io.github.oybek.playcs.service.Manager
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.util.Random

class ManagerImpl[F[_]: Sync: Timer: Clock](consolePoolRef: Ref[F, ConsolePool[F]]) extends Manager[F] {
  override def findConsole(chatId: Long): F[Option[WithMeta[ConsoleHigh[F], ConsoleMeta]]] =
    for {
      ConsolePool(_, busyConsoles) <- consolePoolRef.get
      consoleOpt = busyConsoles.find(_.meta.usingBy == chatId)
    } yield consoleOpt

  override def expireCheck: F[Unit] =
    for {
      _ <- log.info("checking pool for expired consoles...")
      now <- Clock[F].instantNow
      ConsolePool(freeConsoles, busyConsoles) <- consolePoolRef.get
      (expiredConsoles, stillBusy) = busyConsoles.partition(_.meta.deadline.isBefore(now))
      freedConsoles <- expiredConsoles.traverse {
        case console WithMeta _ =>
          resetConsole(console, randomPassword).as(console)
      }
      _ <- consolePoolRef.set(ConsolePool(freeConsoles ++ freedConsoles, stillBusy))
      _ <- log.info(s"consoles on ports ${freedConsoles.map(_.port)} is freed")
    } yield ()

  override def rentConsole(chatId: Long,
                           ttl: FiniteDuration): F[Either[String, ConsoleHigh[F] WithMeta ConsoleMeta]] =
    consolePoolRef.get.flatMap {
      case ConsolePool(Nil, busyConsoles) =>
        busyConsoles.find(_.meta.usingBy == chatId).fold(
          "Кончилась оперативка на серваке - напишите @wolfodav"
            .asLeft[ConsoleHigh[F] WithMeta ConsoleMeta]
            .pure[F]
        )(_.asRight[String].pure[F])

      case ConsolePool(console::consoles, busyConsoles) =>
        busyConsoles.find(_.meta.usingBy == chatId).fold(
          for {
            now <- Clock[F].instantNow
            consoleMeta = ConsoleMeta(
              password = randomPassword,
              usingBy = chatId,
              deadline = now.plusSeconds(ttl.toSeconds)
            )
            _ <- resetConsole(console, consoleMeta.password)
            _ <- log.info(s"console on port=${console.port} is rented by $chatId until ${consoleMeta.deadline}")
            rentedConsole = console.withMeta(consoleMeta)
            _ <- consolePoolRef.set(
              ConsolePool(consoles, rentedConsole::busyConsoles)
            )
          } yield rentedConsole.asRight[String]
        )(_.asRight[String].pure[F])
    }

  override def freeConsole(chatId: Long): F[Unit] =
    for {
      ConsolePool(freeConsoles, busyConsoles) <- consolePoolRef.get
      now <- Clock[F].instantNow
      _ <- consolePoolRef.set(
        ConsolePool(
          freeConsoles,
          busyConsoles
            .map {
              case console WithMeta meta =>
                console withMeta (
                  if (meta.usingBy == chatId) meta.copy(deadline = now) else meta
                )
            }
        )
      )
      _ <- expireCheck
    } yield ()

  override def status: F[String] =
    for {
      ConsolePool(freeConsoles, _) <- consolePoolRef.get
    } yield s"Свободных серверов: ${freeConsoles.length}"

  private def resetConsole(console: ConsoleHigh[F], password: String): F[Unit] =
    for {
      _ <- console.svPassword(password)
      _ <- Timer[F].sleep(200.millis)
      _ <- console.map("de_dust2")
      _ <- Timer[F].sleep(200.millis)
    } yield ()

  private def randomPassword: String = (Random.nextInt(passwordUpperBorder) + passwordOffset).toString
  private val passwordUpperBorder = 9000
  private val passwordOffset = 1000

  private val log = Slf4jLogger.getLoggerFromName[F]("Manager")
}
