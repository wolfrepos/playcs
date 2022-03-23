package io.github.oybek.service.impl

import cats.Monad
import cats.MonadThrow
import cats.implicits.*
import cats.~>
import io.github.oybek.common.ListOps.firstSomeF
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
import io.github.oybek.service.Console
import io.github.oybek.service.HldsConsole
import io.github.oybek.service.PasswordGenerator
import telegramium.bots.ChatIntId
import telegramium.bots.Markdown

import java.time.Duration
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.DurationInt
import scala.concurrent.duration.FiniteDuration

class ConsoleImpl[F[_]: MonadThrow: Clock, G[_]: Monad](consolePoolManager: PoolManager[F, HldsConsole[F], ChatIntId],
                                                        passwordGenerator: PasswordGenerator[F],
                                                        adminDao: AdminDao[G],
                                                        tx: G ~> F,
                                                        log: ContextLogger[F]) extends Console[F]:

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
    List(find(chatId), rent(chatId)).firstSomeF.flatMap {
      case None =>
        List(SendText(chatId, "No free server left, contact t.me/turtlebots")).pure[F]

      case Some(console) =>
        for
          pass <- passwordGenerator.generate
          _ <- console.svPassword(pass)
          _ <- console.changeLevel(map.getOrElse("de_dust2"))
        yield 
          List(
            SendText(chatId, "Your server is ready. Copy paste this"),
            Sleep(200.millis),
            sendConsole(chatId, console, pass)
          )
    }

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
