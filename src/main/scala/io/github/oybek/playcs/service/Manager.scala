package io.github.oybek.playcs.service

import cats.effect.concurrent.Ref
import cats.effect.{Concurrent, Sync, Timer}
import cats.implicits.toTraverseOps
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.github.oybek.common.WithMeta
import io.github.oybek.config.Config
import io.github.oybek.console.service.ConsoleHigh
import io.github.oybek.playcs.model.{ConsoleMeta, ConsolePool}
import io.github.oybek.playcs.service.impl.ManagerImpl

import java.io.File
import scala.concurrent.duration.FiniteDuration

trait Manager[F[_]] {
  def expireCheck: F[Unit]
  def rentConsole(chatId: Long,
                  ttl: FiniteDuration): F[Either[String, ConsoleHigh[F] WithMeta ConsoleMeta]]
  def freeConsole(chatId: Long): F[Unit]
  def findConsole(chatId: Long): F[Option[ConsoleHigh[F] WithMeta ConsoleMeta]]
  def status: F[String]
}

object Manager {
  def create[F[_]: Sync: Timer: Concurrent](consoles: List[ConsoleHigh[F]]): F[Manager[F]] = {
    for {
      consolePool <- Ref.of[F, ConsolePool[F]](
        ConsolePool[F](
          free = consoles,
          busy = List.empty[ConsoleHigh[F] WithMeta ConsoleMeta]
        )
      )
    } yield new ManagerImpl[F](consolePool)
  }
}
