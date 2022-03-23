package io.github.oybek.service.impl

import cats.effect.Ref
import cats.implicits.{catsSyntaxApplicativeErrorId, catsSyntaxApplicativeId, catsSyntaxOptionId, toTraverseOps}
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.{Applicative, MonadThrow}
import io.github.oybek.common.WithMeta
import io.github.oybek.common.logger.{Context, ContextLogger}
import io.github.oybek.common.withMeta
import io.github.oybek.common.time.Clock
import io.github.oybek.exception.BusinessException.NoFreeConsolesException
import io.github.oybek.model.Reaction.SendText
import io.github.oybek.model.{ConsoleMeta, ConsolePool, Reaction}
import io.github.oybek.service.{HldsConsole, HldsConsolePoolManager, PasswordGenerator}
import telegramium.bots.ChatIntId

import scala.concurrent.duration.FiniteDuration

class HldsConsolePoolManagerImpl[F[_]: Applicative: MonadThrow: Clock, G[_]]
                                (consolePoolRef: Ref[F, ConsolePool[F]],
                                 passwordGenerator: PasswordGenerator[F],
                                 log: ContextLogger[F]) extends HldsConsolePoolManager[F] {
  override def findConsole(chatId: ChatIntId): Context[F[Option[WithMeta[HldsConsole[F], ConsoleMeta]]]] =
    for
      ConsolePool(_, busyConsoles) <- consolePoolRef.get
      consoleOpt = busyConsoles.find(_.meta.usingBy == chatId)
    yield consoleOpt

  override def getConsolesWith(prop: ConsoleMeta => Boolean): Context[F[List[HldsConsole[F] WithMeta ConsoleMeta]]] =
    for
      ConsolePool(_, busyConsoles) <- consolePoolRef.get
      result = busyConsoles.filter(x => prop(x.meta))
    yield result

  override def rentConsole(chatId: ChatIntId, ttl: FiniteDuration): Context[F[HldsConsole[F] WithMeta ConsoleMeta]] =
    for
      ConsolePool(freeConsoles, busyConsoles) <- consolePoolRef.get
      allConsoles <- freeConsoles match
        case Nil =>
          MonadThrow[F].raiseError(
            NoFreeConsolesException(
              List(SendText(chatId, "Can't create new server, write t.me/turtlebots"))))
        case x::xs => (x, xs).pure[F]
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

  override def freeConsole(chatIds: ChatIntId*): Context[F[Unit]] =
    for
      ConsolePool(freeConsoles, busyConsoles) <- consolePoolRef.get
      (toSetFree, leftBusy) = busyConsoles.partition(x => chatIds.contains(x.meta.usingBy))
      toSetFreeConsoles = toSetFree.map(_.get)
      _ <- toSetFreeConsoles.traverse(changePasswordAndKickAll(_))
      _ <- consolePoolRef.set(ConsolePool(freeConsoles ++ toSetFreeConsoles, leftBusy))
      _ <- log.info(s"consoles on ports ${toSetFreeConsoles.map(_.port)} has been released")
    yield ()

  private def changePasswordAndKickAll(console: HldsConsole[F], password: Option[String] = None): F[Unit] =
    for
      fallBackPassword <- passwordGenerator.generate
      _ <- console.svPassword(password.getOrElse(fallBackPassword))
      _ <- console.map("de_dust2")
    yield ()
}
