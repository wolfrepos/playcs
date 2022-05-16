package io.github.oybek.hub

import cats.Monad
import cats.MonadThrow
import cats.data.EitherT
import cats.data.NonEmptyList
import cats.implicits.*
import cats.syntax.apply
import cats.~>
import io.github.oybek.common.PoolManager
import io.github.oybek.common.With
import io.github.oybek.common.logger.Context
import io.github.oybek.common.logger.ContextData
import io.github.oybek.common.logger.ContextLogger
import io.github.oybek.cstrike.model.Command
import io.github.oybek.cstrike.model.Command.*
import io.github.oybek.cstrike.parser.CommandParser
import io.github.oybek.hlds.HldsConsole
import io.github.oybek.hub.Hub
import io.github.oybek.model.Reaction
import io.github.oybek.model.Reaction.SendText
import io.github.oybek.model.Reaction.Sleep
import io.github.oybek.password.PasswordGenerator
import telegramium.bots.ChatIntId
import telegramium.bots.Markdown
import telegramium.bots.Markdown2
import telegramium.bots.User

import java.time.Duration
import java.time.OffsetDateTime
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.DurationInt
import scala.concurrent.duration.FiniteDuration
import cats.data.OptionT

trait Hub[F[_]]:
  def handle(chatId: ChatIntId,
             user: User,
             text: String): Context[F[List[Reaction]]]

object Hub:
  def create[F[_]: MonadThrow: ContextLogger, G[_]: Monad]
            (consolePool: PoolManager[F, HldsConsole[F], ChatIntId],
             passwordGenerator: PasswordGenerator[F],
             tx: G ~> F): Hub[F] = 
    new Hub[F]:
      override def handle(chatId: ChatIntId,
                          user: User,
                          text: String): Context[F[List[Reaction]]] =
        ContextLogger[F].info(s"Got message $text") >> {
          CommandParser.parse(text) match
            case       _: String  => confusedMessage(chatId)
            case command: Command => handleCommand(chatId, user, command)
        }

      private def handleCommand(chatId: ChatIntId,
                                user: User,
                                command: Command): Context[F[List[Reaction]]] =
        command match
          case NewCommand(map)         => handleNewCommand(chatId, map)
          case FreeCommand             => handleFreeCommand(chatId)
          case HelpCommand             => handleHelpCommand(chatId)
          case SayCommand(text)        => handleSayCommand(chatId, text)
          case _                       => List(SendText(chatId, "Еще не реализовано"): Reaction).pure[F]

      private def handleNewCommand(chatId: ChatIntId, map: Option[String]): Context[F[List[Reaction]]] =
        val caseHave: OptionT[F, List[Reaction]] =
          for
            console <- OptionT(consolePool.find(chatId))
            _       <- OptionT.liftF(console.changeLevel(map.getOrElse("de_dust2")))
          yield List(SendText(chatId, "You already have the server, just changing a map"))

        val caseRent: OptionT[F, List[Reaction]] =
          for
            console <- OptionT(consolePool.rent(chatId))
            pass    <- OptionT.liftF(passwordGenerator.generate)
            _       <- OptionT.liftF(console.svPassword(pass))
            _       <- OptionT.liftF(console.changeLevel(map.getOrElse("de_dust2")))
            actions = List(
              SendText(chatId, "Your server is ready. Copy paste this"),
              Sleep(200.millis),
              sendConsole(chatId, console, pass)
            )
          yield actions

        val noFreeServerLeft =
          List(SendText(chatId, "No free server left, contact t.me/turtlebots"))

        caseHave.orElse(caseRent).getOrElse(noFreeServerLeft)

      private def handleFreeCommand(chatId: ChatIntId): Context[F[List[Reaction]]] =
        consolePool.find(chatId).flatMap {
          case Some(_) =>
            consolePool.free(chatId).as(
              List(SendText(chatId, "Server has been deleted")))

          case None =>
            List(SendText(chatId, "No created servers"): Reaction).pure[F]
        }

      private def handleHelpCommand(chatId: ChatIntId): F[List[Reaction]] =
        List(SendText(chatId, helpText): Reaction).pure[F]

      private def handleSayCommand(chatId: ChatIntId, text: String): Context[F[List[Reaction]]] =
        consolePool.find(chatId).flatMap {
          case None =>
            List(SendText(chatId, "Create a server first (/help)")).pure[F]
          case Some(console) =>
            console.say(text).as(List.empty[Reaction])
        }

      private def sendConsole(chatId: ChatIntId, console: HldsConsole[F], password: String): SendText =
        SendText(chatId, s"`connect ${console.ip}:${console.port}; password $password`", Markdown.some)

      private def confusedMessage(chatId: ChatIntId): F[List[Reaction]] =
        List(SendText(chatId, "What? /help"): Reaction).pure[F]
