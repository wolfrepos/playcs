package io.github.oybek.playcs.service

import cats.Monad
import cats.MonadThrow
import cats.data.EitherT
import cats.data.NonEmptyList
import cats.data.OptionT
import cats.implicits.*
import cats.syntax.apply
import cats.~>
import io.github.oybek.playcs.client.HldsClient
import io.github.oybek.playcs.common.Pool
import io.github.oybek.playcs.common.logger.Context
import io.github.oybek.playcs.common.logger.ContextData
import io.github.oybek.playcs.common.logger.ContextLogger
import io.github.oybek.playcs.cstrike.model.Command
import io.github.oybek.playcs.cstrike.model.Command.*
import io.github.oybek.playcs.cstrike.parser.CommandParser
import io.github.oybek.playcs.dto.Reaction
import io.github.oybek.playcs.dto.Reaction.SendText
import io.github.oybek.playcs.dto.Reaction.Sleep
import io.github.oybek.playcs.service.Hub
import io.github.oybek.playcs.service.PasswordService
import telegramium.bots.ChatIntId
import telegramium.bots.Markdown
import telegramium.bots.Markdown2
import telegramium.bots.User

import java.time.Duration
import java.time.OffsetDateTime
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.DurationInt
import scala.concurrent.duration.FiniteDuration

trait Hub[F[_]]:
  def handle(
      chatId: ChatIntId,
      user: User,
      text: String
  ): Context[F[List[Reaction]]]

object Hub:
  def create[F[_]: MonadThrow: ContextLogger](
      hldsPool: Pool[F, HldsClient[F]],
      passwordGenerator: PasswordService[F]
  ): Hub[F] =
    new Hub[F]:
      override def handle(
          chatId: ChatIntId,
          user: User,
          text: String
      ): Context[F[List[Reaction]]] =
        ContextLogger[F].info(s"Got message $text") >> (
          CommandParser.parse(text) match
            case NewCommand(map)  => handleNewCommand(chatId, map.getOrElse("de_dust2"))
            case FreeCommand      => handleFreeCommand(chatId)
            case HelpCommand      => handleHelpCommand(chatId)
            case SayCommand(text) => handleSayCommand(chatId, text)
            case _                => confusedMessage(chatId)
        )

      private def handleNewCommand(
          chatId: ChatIntId,
          map: String
      ): Context[F[List[Reaction]]] = {
        for
          hlds <- OptionT(hldsPool.find(chatId.id))
          _ <- OptionT.liftF(hlds.changeLevel(map))
        yield List(
          SendText(chatId, "You already got the server, just changing a map")
        )
      } orElse {
        for
          hlds <- OptionT(hldsPool.rent(chatId.id))
          pass <- OptionT.liftF(passwordGenerator.generate)
          _ <- OptionT.liftF(hlds.svPassword(pass))
          _ <- OptionT.liftF(hlds.changeLevel(map))
        yield List(
          SendText(chatId, "Your server is ready. Copy paste this"),
          Sleep(200.millis),
          sendConsole(chatId, hlds, pass)
        )
      } getOrElse {
        List(SendText(chatId, "No free server left, contact t.me/turtlebots"))
      }

      private def handleFreeCommand(
          chatId: ChatIntId
      ): Context[F[List[Reaction]]] =
        hldsPool.find(chatId.id).flatMap {
          case Some(_) =>
            hldsPool
              .free(chatId.id)
              .as(List(SendText(chatId, "Server has been deleted")))

          case None =>
            List(SendText(chatId, "No created servers"): Reaction).pure[F]
        }

      private def handleHelpCommand(chatId: ChatIntId): F[List[Reaction]] =
        List(SendText(chatId, helpText): Reaction).pure[F]

      private def handleSayCommand(
          chatId: ChatIntId,
          text: String
      ): Context[F[List[Reaction]]] =
        hldsPool.find(chatId.id).flatMap {
          case None =>
            List(SendText(chatId, "Create a server first (/help)")).pure[F]
          case Some(console) =>
            console.say(text).as(List.empty[Reaction])
        }

      private def sendConsole(
          chatId: ChatIntId,
          console: HldsClient[F],
          password: String
      ): SendText =
        SendText(
          chatId,
          s"`connect ${console.ip}:${console.port}; password $password`",
          Markdown.some
        )

      private def confusedMessage(chatId: ChatIntId): F[List[Reaction]] =
        List(SendText(chatId, "What? /help"): Reaction).pure[F]
