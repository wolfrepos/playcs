package io.github.oybek.playcs.bot

import cats.Parallel
import cats.effect.{Sync, Timer}
import cats.implicits.{catsSyntaxApplicativeId, catsSyntaxOptionId}
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.github.oybek.common.WithMeta
import io.github.oybek.console.service.ConsoleHigh
import io.github.oybek.cstrike.model.Command.{FreeCommand, HelpCommand, JoinCommand, NewCommand, StatusCommand, helpText}
import io.github.oybek.cstrike.service.Translator
import io.github.oybek.playcs.model.ConsoleMeta
import io.github.oybek.playcs.service.Manager
import telegramium.bots.high.implicits.methodOps
import telegramium.bots.high.{Api, LongPollBot, Methods}
import telegramium.bots.{ChatIntId, Markdown, Message}

import scala.concurrent.duration.{DurationInt, FiniteDuration}

class Bot[F[_]: Sync: Timer: Parallel](api: Api[F],
                                       manager: Manager[F],
                                       translator: Translator) extends LongPollBot[F](api) {

  // scalastyle:off
  implicit private val apiImplicit: Api[F] = api
  // scalastyle:on

  override def onMessage(message: Message): F[Unit] =
    message.text.fold(().pure[F]) { text =>
      onTextMessage(ChatIntId(message.chat.id), text)
    }

  def onTextMessage(chatId: ChatIntId, text: String): F[Unit] = {
    translator.translate(text) match {
      case Left(_) =>
        send(chatId, "Че? (/help)")

      case Right(NewCommand(map, ttl)) =>
        newCommandHandler(chatId, map, ttl)

      case Right(JoinCommand) =>
        joinCommandHandler(chatId)

      case Right(StatusCommand) =>
        statusCommandHandler(chatId)

      case Right(FreeCommand) =>
        freeCommandHandler(chatId)

      case Right(HelpCommand) =>
        send(chatId, helpText)

      case _ =>
        send(chatId, "Еще не реализовано")
    }
  }

  private def freeCommandHandler(chatId: ChatIntId) =
    for {
      _ <- manager.freeConsole(chatId.id)
      _ <- send(chatId, "Сервер освобожден")
    } yield ()

  private def statusCommandHandler(chatId: ChatIntId) =
    for {
      status <- manager.status
      _ <- send(chatId, status)
    } yield ()

  private def joinCommandHandler(chatId: ChatIntId) =
    for {
      consoleOpt <- manager.findConsole(chatId.id)
      _ <- consoleOpt.fold(send(chatId, "Создай сервер сначала (/help)"))(sendConsole(chatId, _))
    } yield ()

  private def newCommandHandler(chatId: ChatIntId, map: String, ttl: FiniteDuration) =
    manager.rentConsole(chatId.id, ttl).flatMap {
      case Left(errorText) =>
        send(chatId, errorText)

      case Right(console) =>
        for {
          _ <- console.get.changeLevel(map)
          _ <- send(chatId, "Сервер создан. Скопируй в консоль это")
          _ <- Timer[F].sleep(200.millis)
          _ <- sendConsole(chatId, console)
        } yield ()
    }

  private def send(chatId: ChatIntId, text: String): F[Unit] =
    Methods.sendMessage(
      chatId = chatId,
      text = text,
    ).exec.void

  private def sendConsole(chatId: ChatIntId, console: ConsoleHigh[F] WithMeta ConsoleMeta): F[Unit] =
    console match {
      case console WithMeta ConsoleMeta(password, _, _) =>
        Methods.sendMessage(
          chatId = chatId,
          text = s"`connect ${console.ip}:${console.port}; password $password`",
          parseMode = Markdown.some
        ).exec.void
    }
}
