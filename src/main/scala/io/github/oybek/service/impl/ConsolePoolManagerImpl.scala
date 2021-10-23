package io.github.oybek.service.impl

import cats.{Id, Monad}
import cats.effect.concurrent.Ref
import cats.effect.{Clock, Timer}
import cats.implicits.{catsSyntaxApplicativeId, catsSyntaxEitherId, toTraverseOps}
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.chrisdavenport.log4cats.{MessageLogger, SelfAwareStructuredLogger}
import io.github.oybek.common.WithMeta
import io.github.oybek.common.WithMeta.toMetaOps
import io.github.oybek.model.{ConsoleMeta, ConsolePool}
import io.github.oybek.service.{ConsolePoolManager, HldsConsole, PasswordGenerator}

import scala.concurrent.duration.{DurationInt, FiniteDuration}

class ConsolePoolManagerImpl[F[_]: Monad: Timer](consolePoolRef: Ref[F, ConsolePool[F]],
                                                 passwordGenerator: PasswordGenerator[F],
                                                 log: MessageLogger[F]) extends ConsolePoolManager[F] {
  override def findConsole(chatId: Long): F[Option[WithMeta[HldsConsole[F], ConsoleMeta]]] = {
    for {
      consolePool <- consolePoolRef.get
      ConsolePool(_, busyConsoles) = consolePool
      consoleOpt = busyConsoles.find(_.meta.usingBy == chatId)
    } yield consoleOpt
  }

  override def expireCheck: F[Unit] =
    for {
      _ <- log.info("checking pool for expired consoles...")
      now <- Clock[F].instantNow
      consolePool <- consolePoolRef.get
      ConsolePool(freeConsoles, busyConsoles) = consolePool
      (expiredConsoles, stillBusy) = busyConsoles.partition(_.meta.deadline.isBefore(now))
      password <- passwordGenerator.generate
      freedConsoles <- expiredConsoles.traverse {
        case console WithMeta _ =>
          resetConsole(console, password).as(console)
      }
      _ <- consolePoolRef.set(ConsolePool(freeConsoles ++ freedConsoles, stillBusy))
      _ <- log.info(s"consoles on ports ${freedConsoles.map(_.port)} is freed")
    } yield ()

  override def rentConsole(chatId: Long,
                           ttl: FiniteDuration): F[Either[String, HldsConsole[F] WithMeta ConsoleMeta]] =
    consolePoolRef.get.flatMap {
      case ConsolePool(Nil, busyConsoles) =>
        busyConsoles.find(_.meta.usingBy == chatId).fold(
          "Кончилась оперативка на серваке - напишите @wolfodav"
            .asLeft[HldsConsole[F] WithMeta ConsoleMeta]
            .pure[F]
        )(_.asRight[String].pure[F])

      case ConsolePool(console::consoles, busyConsoles) =>
        busyConsoles.find(_.meta.usingBy == chatId).fold(
          for {
            now <- Clock[F].instantNow
            password <- passwordGenerator.generate
            consoleMeta = ConsoleMeta(
              password = password,
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
      consolePool <- consolePoolRef.get
      ConsolePool(freeConsoles, busyConsoles) = consolePool
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
      consolePool <- consolePoolRef.get
      ConsolePool(freeConsoles, _) = consolePool
    } yield s"Свободных серверов: ${freeConsoles.length}"

  private def resetConsole(console: HldsConsole[F], password: String): F[Unit] =
    for {
      _ <- console.svPassword(password)
      _ <- Timer[F].sleep(200.millis)
      _ <- console.map("de_dust2")
      _ <- Timer[F].sleep(200.millis)
    } yield ()
}
