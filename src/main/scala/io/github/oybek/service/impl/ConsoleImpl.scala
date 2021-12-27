package io.github.oybek.service.impl

import cats.Monad
import cats.effect.Timer
import cats.implicits.{catsSyntaxApplicativeId, catsSyntaxOptionId, toFlatMapOps, toFunctorOps}
import io.github.oybek.common.WithMeta
import io.github.oybek.cstrike.model.Command._
import io.github.oybek.cstrike.parser.CommandParser
import io.github.oybek.model.Reaction.{SendText, Sleep}
import io.github.oybek.model.{ConsoleMeta, Reaction}
import io.github.oybek.service.{Console, ConsolePoolManager, HldsConsole}
import telegramium.bots.{ChatIntId, Markdown}

import scala.concurrent.duration.DurationInt

class ConsoleImpl[F[_]: Monad: Timer](consolePoolManager: ConsolePoolManager[F]) extends Console[F] {

  def handle(chatId: ChatIntId, text: String): F[List[Reaction]] = {
    CommandParser.parse(text) match {
      case Right(NewCommand(map)) => handleNewCommand(chatId, map)
      case Right(JoinCommand)     => handleJoinCommand(chatId)
      case Right(BalanceCommand)  => handleStatusCommand(chatId)
      case Right(FreeCommand)     => handleFreeCommand(chatId)
      case Right(HelpCommand)     => List(SendText(chatId, helpText): Reaction).pure[F]
      case Right(_)               => List(SendText(chatId, "Еще не реализовано"): Reaction).pure[F]
      case Left(_)                => List(SendText(chatId, "Че? (/help)"): Reaction).pure[F]
    }
  }

  private def handleNewCommand(chatId: ChatIntId, map: String): F[List[Reaction]] =
    consolePoolManager.rentConsole(chatId.id, 15.minutes).flatMap {
      case Left(errorText) =>
        List(SendText(chatId, errorText): Reaction).pure[F]

      case Right(console WithMeta ConsoleMeta(password, _, _))  =>
        console
          .changeLevel(map)
          .as(List(
            SendText(chatId, "Сервер создан. Скопируй в консоль это"),
            Sleep(200.millis),
            sendConsole(chatId, console, password)
          ))
    }

  private def handleJoinCommand(chatId: ChatIntId): F[List[Reaction]] =
    consolePoolManager.findConsole(chatId.id).map {
      case None =>
        List(SendText(chatId, "Создай сервер сначала (/help)"))

      case Some(console WithMeta ConsoleMeta(password, _, _))  =>
        List(sendConsole(chatId, console, password))
    }

  private def handleStatusCommand(chatId: ChatIntId): F[List[Reaction]] =
    consolePoolManager
      .status
      .map(x => List(SendText(chatId, x)))

  private def handleFreeCommand(chatId: ChatIntId): F[List[Reaction]] =
    consolePoolManager.freeConsole(chatId.id)
      .as(List(SendText(chatId, "Сервер освобожден")))

  private def sendConsole(chatId: ChatIntId, console: HldsConsole[F], password: String): SendText = {
    SendText(chatId, s"`connect ${console.ip}:${console.port}; password $password`", Markdown.some)
  }
}
