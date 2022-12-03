package io.github.oybek.playcs.service

import cats.Monad
import cats.MonadThrow
import cats.data.EitherT
import cats.data.NonEmptyList
import cats.data.OptionT
import cats.effect.IO
import cats.implicits.*
import cats.syntax.apply
import cats.~>
import io.github.oybek.playcs.client.HldsClient
import io.github.oybek.playcs.common.Pool
import io.github.oybek.playcs.domain.Command.*
import io.github.oybek.playcs.domain.Command
import io.github.oybek.playcs.dto.Reaction
import io.github.oybek.playcs.dto.Reaction.*
import io.github.oybek.playcs.service.Bot
import io.github.oybek.playcs.generatePassword
import mouse.all.*
import telegramium.bots.ChatIntId
import telegramium.bots.Markdown
import telegramium.bots.Markdown2
import telegramium.bots.User

import java.time.Duration
import java.time.OffsetDateTime
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.DurationInt
import scala.concurrent.duration.FiniteDuration

trait Bot:
  def handle(
      chatId: ChatIntId,
      text: String
  ): IO[List[Reaction]]

object Bot:
  def create(
      hldsTimeout: FiniteDuration,
      hldsPool: Pool[IO, HldsClient]
  ): Bot =
    new Bot:
      override def handle(
          chatId: ChatIntId,
          text: String
      ): IO[List[Reaction]] =
        Command.parse(text) match
          case NewCommand(map)  => handleNewCommand(chatId, map.getOrElse("de_dust2"))
          case FreeCommand      => handleFreeCommand(chatId)
          case HelpCommand      => handleHelpCommand(chatId)
          case SayCommand(text) => handleSayCommand(chatId, text)
          case _                => confusedMessage(chatId)

      private def handleNewCommand(
          chatId: ChatIntId,
          map: String
      ): IO[List[Reaction]] = {
        for
          hlds <- hldsPool.find(chatId.id) |> (OptionT(_))
          _ <- hlds.changeLevel(map) |> (OptionT.liftF(_))
        yield List(
          SendText(chatId, "You already got the server, just changing a map")
        )
      } orElse {
        for
          hlds <- hldsPool.rent(chatId.id) |> (OptionT(_))
          pass <- generatePassword |> (OptionT.liftF(_))
          _ <- hlds.svPassword(pass) |> (OptionT.liftF(_))
          _ <- hlds.changeLevel(map) |> (OptionT.liftF(_))
        yield List(
          SendText(
            chatId,
            "Your server is created " +
              s"(after ${hldsTimeout.toMinutes} minutes it will be deleted). " +
              "Copy paste next line to game console"
          ),
          Sleep(200.millis),
          sendConsole(chatId, hlds, pass),
          Sleep(hldsTimeout),
          Receive(chatId, "/free")
        )
      } getOrElse {
        List(SendText(chatId, "No free server left, contact t.me/turtlebots"))
      }

      private def handleFreeCommand(
          chatId: ChatIntId
      ): IO[List[Reaction]] =
        hldsPool.find(chatId.id).flatMap {
          case Some(_) =>
            hldsPool
              .free(chatId.id)
              .as(List(SendText(chatId, "Server has been deleted")))

          case None =>
            List(SendText(chatId, "No created servers"): Reaction).pure[IO]
        }

      private def handleHelpCommand(chatId: ChatIntId): IO[List[Reaction]] =
        List(SendText(chatId, helpText): Reaction).pure[IO]

      private def handleSayCommand(
          chatId: ChatIntId,
          text: String
      ): IO[List[Reaction]] =
        hldsPool.find(chatId.id).flatMap {
          case None =>
            List(SendText(chatId, "Create a server first (/help)")).pure[IO]
          case Some(console) =>
            console.say(text).as(List.empty[Reaction])
        }

      private def sendConsole(
          chatId: ChatIntId,
          console: HldsClient,
          password: String
      ): SendText =
        SendText(
          chatId,
          s"`connect ${console.ip}:${console.port}; password $password`",
          Markdown.some
        )

      private def confusedMessage(chatId: ChatIntId): IO[List[Reaction]] =
        List(SendText(chatId, "What? /help"): Reaction).pure[IO]
