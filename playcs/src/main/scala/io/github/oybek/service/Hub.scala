package io.github.oybek.service

import cats.Monad
import cats.MonadThrow
import cats.data.EitherT
import cats.implicits.*
import cats.implicits.*
import cats.syntax.apply
import cats.~>
import io.github.oybek.common.PoolManager
import io.github.oybek.common.With
import io.github.oybek.common.logger.Context
import io.github.oybek.common.logger.ContextData
import io.github.oybek.common.logger.ContextLogger
import io.github.oybek.common.time.Clock
import io.github.oybek.cstrike.model.Command
import io.github.oybek.cstrike.model.Command.*
import io.github.oybek.cstrike.parser.CommandParser
import io.github.oybek.database.dao.AdminDao
import io.github.oybek.exception.BusinessException.UnathorizedException
import io.github.oybek.exception.BusinessException.ZeroBalanceException
import io.github.oybek.model.Reaction
import io.github.oybek.model.Reaction.SendText
import io.github.oybek.model.Reaction.Sleep
import io.github.oybek.service.HldsConsole
import io.github.oybek.service.Hub
import io.github.oybek.service.PasswordGenerator
import mouse.foption.FOptionSyntaxMouse
import telegramium.bots.ChatIntId
import telegramium.bots.Markdown

import java.time.Duration
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.DurationInt
import scala.concurrent.duration.FiniteDuration

trait Hub[F[_]]:
  def handle(chatId: ChatIntId, text: String): Context[F[List[Reaction]]]

object Hub:
  def create[F[_]: MonadThrow: Clock, G[_]: Monad](consolePoolManager: PoolManager[F, HldsConsole[F], ChatIntId],
                                                 passwordGenerator: PasswordGenerator[F],
                                                 adminDao: AdminDao[G],
                                                 tx: G ~> F,
                                                 log: ContextLogger[F]): Hub[F] = 
    new Hub[F]:
      override def handle(chatId: ChatIntId, text: String): Context[F[List[Reaction]]] =
        log.info(s"Got message $text") >> (
          CommandParser.parse(text) match
            case _: String => confusedMessage(chatId)
            case command: Command => handleCommand(chatId, command)
        )

      private def handleCommand(chatId: ChatIntId, command: Command): Context[F[List[Reaction]]] =
        command match
          case NewCommand(map)  => handleNewCommand(chatId, map)
          case FreeCommand      => handleFreeCommand(chatId)
          case HelpCommand      => handleHelpCommand(chatId)
          case SayCommand(text) => handleSayCommand(chatId, text)
          case _                => List(SendText(chatId, "Еще не реализовано"): Reaction).pure[F]

      private def handleNewCommand(chatId: ChatIntId, map: Option[String]): Context[F[List[Reaction]]] =
        import consolePoolManager.{find, rent}

        def caseFind: F[Option[List[Reaction]]] =
          find(chatId).flatMap(_.traverse(console =>
            console.changeLevel(map.getOrElse("de_dust2")).as(
              List(SendText(chatId, "You already got the server, just changing a map")))
          ))

        def caseRent: F[Option[List[Reaction]]] =
          rent(chatId).flatMap(_.traverse(console =>
            for
              pass <- passwordGenerator.generate
              _ <- console.svPassword(pass)
              _ <- console.changeLevel(map.getOrElse("de_dust2"))
            yield 
              List(
                SendText(chatId, "Your server is ready. Copy paste this"),
                Sleep(200.millis),
                sendConsole(chatId, console, pass
              ))
          ))

        val caseNone: List[Reaction] =
          List(SendText(chatId, "No free server left, contact t.me/turtlebots"))

        caseFind.orElseF(caseRent).getOrElse(caseNone)

      private def handleFreeCommand(chatId: ChatIntId): Context[F[List[Reaction]]] =
        consolePoolManager.find(chatId).flatMap {
          case Some(_) =>
            consolePoolManager.free(chatId).as(
              List(SendText(chatId, "Server has been deleted")))

          case None =>
            List(SendText(chatId, "No created servers"): Reaction).pure[F]
        }

      private def handleHelpCommand(chatId: ChatIntId): F[List[Reaction]] =
        List(SendText(chatId, helpText): Reaction).pure[F]

      private def handleSayCommand(chatId: ChatIntId, text: String): Context[F[List[Reaction]]] =
        consolePoolManager.find(chatId).flatMap {
          case None =>
            List(SendText(chatId, "Create a server first (/help)")).pure[F]
          case Some(console) =>
            console.say(text).as(List.empty[Reaction])
        }

      private def sendConsole(chatId: ChatIntId, console: HldsConsole[F], password: String): SendText =
        SendText(chatId, s"`connect ${console.ip}:${console.port}; password $password`", Markdown.some)

      private def confusedMessage(chatId: ChatIntId): F[List[Reaction]] =
        List(SendText(chatId, "What? /help"): Reaction).pure[F]