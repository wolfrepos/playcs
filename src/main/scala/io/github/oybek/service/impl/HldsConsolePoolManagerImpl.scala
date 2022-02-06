package io.github.oybek.service.impl

import cats.effect.Ref
import cats.implicits.{catsSyntaxApplicativeErrorId, catsSyntaxApplicativeId, catsSyntaxOptionId, toTraverseOps}
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.{Applicative, MonadThrow}
import io.github.oybek.common.WithMeta
import io.github.oybek.common.withMeta
import io.github.oybek.common.time.Clock
import io.github.oybek.exception.BusinessException.NoFreeConsolesException
import io.github.oybek.model.Reaction.SendText
import io.github.oybek.model.{ConsoleMeta, ConsolePool, Reaction}
import io.github.oybek.service.{HldsConsole, HldsConsolePoolManager, PasswordGenerator}
import org.typelevel.log4cats.MessageLogger
import telegramium.bots.ChatIntId

import scala.concurrent.duration.FiniteDuration

class HldsConsolePoolManagerImpl[F[_]: Applicative: MonadThrow: Clock, G[_]]
                                (consolePoolRef: Ref[F, ConsolePool[F]],
                                 passwordGenerator: PasswordGenerator[F],
                                 log: MessageLogger[F]) extends HldsConsolePoolManager[F] {
  override def findConsole(chatId: ChatIntId): F[Option[WithMeta[HldsConsole[F], ConsoleMeta]]] =
    for
      consolePool <- consolePoolRef.get
      ConsolePool(_, busyConsoles) = consolePool
      consoleOpt = busyConsoles.find(_.meta.usingBy == chatId)
    yield consoleOpt

  override def getConsolesWith(prop: ConsoleMeta => Boolean): F[List[HldsConsole[F] WithMeta ConsoleMeta]] =
    for
      consolePool <- consolePoolRef.get
      ConsolePool(_, busyConsoles) = consolePool
      result = busyConsoles.filter(x => prop(x.meta))
    yield result

  override def rentConsole(chatId: ChatIntId, ttl: FiniteDuration): F[HldsConsole[F] WithMeta ConsoleMeta] =
    for
      consolePool <- consolePoolRef.get
      ConsolePool(freeConsoles, busyConsoles) = consolePool
      allConsoles <- freeConsoles match {
        case Nil =>
          MonadThrow[F].raiseError(
            NoFreeConsolesException(
              List(SendText(chatId, "Не осталось свободных серверов"))))
        case x::xs => (x, xs).pure[F]
      }
      (console, consoles) = allConsoles
      now <- Clock[F].instantNow
      password <- passwordGenerator.generate
      consoleMeta = ConsoleMeta(
        password = password,
        usingBy = chatId,
        deadline = now.plusSeconds(ttl.toSeconds))
      _ <- changePasswordAndKickAll(console, consoleMeta.password.some)
      _ <- log.info(s"console on port=${console.port} is rented by $chatId until ${consoleMeta.deadline}")
      rentedConsole = console.withMeta(consoleMeta)
      _ <- consolePoolRef.set(ConsolePool(consoles, rentedConsole::busyConsoles))
    yield rentedConsole

  override def freeConsole(chatIds: ChatIntId*): F[Unit] =
    for
      consolePool <- consolePoolRef.get
      ConsolePool(freeConsoles, busyConsoles) = consolePool
      (toSetFree, leftBusy) = busyConsoles.partition(x => chatIds.contains(x.meta.usingBy))
      toSetFreeConsoles = toSetFree.map(_.get)
      _ <- toSetFreeConsoles.traverse(changePasswordAndKickAll(_))
      _ <- consolePoolRef.set(ConsolePool(freeConsoles ++ toSetFreeConsoles, leftBusy))
    yield ()

  private def changePasswordAndKickAll(console: HldsConsole[F], password: Option[String] = None): F[Unit] =
    for
      fallBackPassword <- passwordGenerator.generate
      _ <- console.svPassword(password.getOrElse(fallBackPassword))
      _ <- console.map("de_dust2")
    yield ()
}
