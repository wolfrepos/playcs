package io.github.oybek.service.impl

import cats.effect.concurrent.Ref
import cats.effect.{Clock, Timer}
import cats.implicits.{catsSyntaxApplicativeId, catsSyntaxEitherId, catsSyntaxOptionId, toTraverseOps}
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.Monad
import io.chrisdavenport.log4cats.MessageLogger
import io.github.oybek.common.WithMeta
import io.github.oybek.common.WithMeta.toMetaOps
import io.github.oybek.model.{ConsoleMeta, ConsolePool}
import io.github.oybek.service.{HldsConsole, HldsConsolePoolManager, PasswordGenerator}

import scala.concurrent.duration.{DurationInt, FiniteDuration}

class HldsConsolePoolManagerImpl[F[_]: Monad: Timer](consolePoolRef: Ref[F, ConsolePool[F]],
                                                     passwordGenerator: PasswordGenerator[F],
                                                     log: MessageLogger[F]) extends HldsConsolePoolManager[F] {
  override def findConsole(chatId: Long): F[Option[WithMeta[HldsConsole[F], ConsoleMeta]]] =
    for {
      ConsolePool(_, busyConsoles) <- consolePoolRef.get
      consoleOpt = busyConsoles.find(_.meta.usingBy == chatId)
    } yield consoleOpt

  override def expireCheck: F[Unit] =
    for {
      _ <- log.info("checking pool for expired consoles...")
      now <- Clock[F].instantNow
      ConsolePool(_, busyConsoles) <- consolePoolRef.get
      expiredConsoles = busyConsoles.filter(_.meta.deadline.isBefore(now))
      _ <- freeConsole(expiredConsoles.map(_.meta.usingBy): _*)
      _ <- log.info(s"consoles on ports ${expiredConsoles.map(_.get.port)} is freed")
    } yield ()

  override def rentConsole(chatId: Long,
                           ttl: FiniteDuration): F[Either[String, HldsConsole[F] WithMeta ConsoleMeta]] =
    consolePoolRef.get.flatMap {
      case ConsolePool(Nil, busyConsoles) =>
        busyConsoles.find(_.meta.usingBy == chatId).fold(
          "Не осталось свободных серверов"
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
            _ <- changePasswordAndKickAll(console, consoleMeta.password.some)
            _ <- log.info(s"console on port=${console.port} is rented by $chatId until ${consoleMeta.deadline}")
            rentedConsole = console.withMeta(consoleMeta)
            _ <- consolePoolRef.set(
              ConsolePool(consoles, rentedConsole::busyConsoles)
            )
          } yield rentedConsole.asRight[String]
        )(_.asRight[String].pure[F])
    }

  override def freeConsole(chatIds: Long*): F[Unit] =
    for {
      ConsolePool(freeConsoles, busyConsoles) <- consolePoolRef.get
      (toSetFree, leftBusy) = busyConsoles.partition(x => chatIds.contains(x.meta.usingBy))
      _ <- toSetFree.traverse(x => changePasswordAndKickAll(x.get))
      _ <- consolePoolRef.set(ConsolePool(freeConsoles ++ toSetFree.map(_.get), leftBusy))
    } yield ()

  private def changePasswordAndKickAll(console: HldsConsole[F], password: Option[String] = None): F[Unit] =
    for {
      fallBackPassword <- passwordGenerator.generate
      _ <- console.svPassword(password.getOrElse(fallBackPassword))
      _ <- Timer[F].sleep(200.millis)
      _ <- console.map("de_dust2")
      _ <- Timer[F].sleep(200.millis)
    } yield ()
}
