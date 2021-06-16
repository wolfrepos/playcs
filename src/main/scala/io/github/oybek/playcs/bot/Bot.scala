package io.github.oybek.playcs.bot

import cats.Parallel
import cats.effect.{Sync, Timer}
import cats.implicits.{catsSyntaxApplicativeId, catsSyntaxOptionId}
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.github.oybek.common.WithMeta
import io.github.oybek.console.service.ConsoleHigh
import io.github.oybek.cstrike.model.Command.{FreeCommand, JoinCommand, NewCommand, StatusCommand}
import io.github.oybek.cstrike.service.Translator
import io.github.oybek.playcs.model.ConsoleMeta
import io.github.oybek.playcs.service.Manager
import telegramium.bots.high.implicits.methodOps
import telegramium.bots.high.{Api, LongPollBot, Methods}
import telegramium.bots.{ChatIntId, Markdown, Message}

import scala.concurrent.duration.DurationInt

class Bot[F[_]: Sync: Timer: Parallel](api: Api[F],
                                       manager: Manager[F],
                                       translator: Translator) extends LongPollBot[F](api) {

  implicit private val apiImplicit = api

  override def onMessage(message: Message): F[Unit] =
    message.text.fold(().pure[F]) { text =>
      onTextMessage(ChatIntId(message.chat.id), text)
    }

  def onTextMessage(chatId: ChatIntId, text: String): F[Unit] = {
    def send(text: String): F[Unit] =
      Methods.sendMessage(
        chatId = chatId,
        text = text,
      ).exec.void

    val sendConsole: (ConsoleHigh[F] WithMeta ConsoleMeta) => F[Unit] = {
      case console WithMeta ConsoleMeta(password, _, _) =>
        Methods.sendMessage(
          chatId = chatId,
          text = s"`connect ${console.ip}:${console.port}; password $password`",
          parseMode = Markdown.some
        ).exec.void
    }

    translator.translate(text) match {
      case Left(parseErrorText) =>
        send(parseErrorText)

      case Right(NewCommand(map, ttl)) =>
        for {
          errorOrConsole <- manager.rentConsole(chatId.id, ttl)
          _ <- errorOrConsole match {
            case Left(errorText) =>
              send(errorText)

            case Right(console) =>
              for {
                _ <- console.get.changeLevel(map)
                _ <- send("Скопируй в консоль")
                _ <- Timer[F].sleep(200.millis)
                _ <- sendConsole(console)
              } yield ()
          }
        } yield ()

      case Right(JoinCommand) =>
        for {
          consoleOpt <- manager.findConsole(chatId.id)
          _ <- consoleOpt.fold(send("Создайте сервер сначала"))(sendConsole)
        } yield ()

      case Right(StatusCommand) =>
        for {
          status <- manager.status
          _ <- send(status)
        } yield ()

      case Right(FreeCommand) =>
        for {
          status <- manager.freeConsole(chatId.id)
          _ <- send("Сервер освобожден")
        } yield ()

      case _ =>
        send("Еще не реализовано")
    }
  }
}
