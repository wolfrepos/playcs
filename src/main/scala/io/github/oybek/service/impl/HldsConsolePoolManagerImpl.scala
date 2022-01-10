package io.github.oybek.service.impl

import cats.effect.Ref
import cats.implicits.{catsSyntaxApplicativeErrorId, catsSyntaxApplicativeId, catsSyntaxOptionId, toTraverseOps}
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.{MonadThrow, ~>}
import io.github.oybek.common.WithMeta
import io.github.oybek.common.WithMeta.toMetaOps
import io.github.oybek.common.time.Clock
import io.github.oybek.database.dao.BalanceDao
import io.github.oybek.database.model.Balance
import io.github.oybek.exception.PoolManagerException.{NoFreeConsolesException, ZeroBalanceException}
import io.github.oybek.model.{ConsoleMeta, ConsolePool}
import io.github.oybek.service.{HldsConsole, HldsConsolePoolManager, PasswordGenerator}
import org.typelevel.log4cats.MessageLogger

import java.time.{Duration, Instant}
import scala.concurrent.duration.DurationInt

class HldsConsolePoolManagerImpl[F[_]: MonadThrow: Clock, G[_]]
                                (consolePoolRef: Ref[F, ConsolePool[F]],
                                 passwordGenerator: PasswordGenerator[F],
                                 balanceDao: BalanceDao[G],
                                 tx: G ~> F,
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

  override def rentConsole(chatId: Long): F[HldsConsole[F] WithMeta ConsoleMeta] =
    for {
      _ <- tx(balanceDao.addIfNotExists(Balance(chatId, 15.minutes.toSeconds)))
      balanceOpt <- tx(balanceDao.findBy(chatId))
      balance <- balanceOpt.fold(
        ZeroBalanceException.raiseError[F, Balance]
      )(_.pure[F])
      ConsolePool(freeConsoles, busyConsoles) <- consolePoolRef.get
      (console, consoles) <- freeConsoles match {
        case Nil => NoFreeConsolesException.raiseError
        case x::xs => (x, xs).pure[F]
      }
      now <- Clock[F].instantNow
      password <- passwordGenerator.generate
      consoleMeta = ConsoleMeta(
        password = password,
        usingBy = chatId,
        deadline = now.plusSeconds(balance.seconds))
      _ <- changePasswordAndKickAll(console, consoleMeta.password.some)
      _ <- log.info(s"console on port=${console.port} is rented by $chatId until ${consoleMeta.deadline}")
      rentedConsole = console.withMeta(consoleMeta)
      _ <- consolePoolRef.set(ConsolePool(consoles, rentedConsole::busyConsoles))
    } yield rentedConsole

  override def freeConsole(chatIds: Long*): F[Unit] =
    for {
      now <- Clock[F].instantNow
      ConsolePool(freeConsoles, busyConsoles) <- consolePoolRef.get
      (toSetFree, leftBusy) = busyConsoles.partition(x => chatIds.contains(x.meta.usingBy))
      _ <- toSetFree.traverse(freeConsoleAndUpdateBalance(now, _))
      _ <- consolePoolRef.set(ConsolePool(freeConsoles ++ toSetFree.map(_.get), leftBusy))
    } yield ()

  private def freeConsoleAndUpdateBalance(now: Instant, consoleWithMeta: HldsConsole[F] WithMeta ConsoleMeta): F[Unit] =
    for {
      console WithMeta meta <- consoleWithMeta.pure[F]
      _ <- changePasswordAndKickAll(console)
      secondsLeft = Duration.between(now, meta.deadline).toSeconds
      _ <- tx(balanceDao.addOrUpdate(Balance(meta.usingBy, secondsLeft)))
    } yield ()

  private def changePasswordAndKickAll(console: HldsConsole[F], password: Option[String] = None): F[Unit] =
    for {
      fallBackPassword <- passwordGenerator.generate
      _ <- console.svPassword(password.getOrElse(fallBackPassword))
      _ <- console.map("de_dust2")
    } yield ()
}
