package io.github.oybek.service

import cats.Monad
import cats.MonadThrow
import cats.data.EitherT
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
import io.github.oybek.database.admin.dao.AdminDao
import io.github.oybek.exception.BusinessException.UnathorizedException
import io.github.oybek.exception.BusinessException.ZeroBalanceException
import io.github.oybek.model.Reaction
import io.github.oybek.model.Reaction.SendText
import io.github.oybek.model.Reaction.Sleep
import io.github.oybek.service.HldsConsole
import io.github.oybek.service.Hub
import io.github.oybek.organizer.model.Will
import io.github.oybek.service.PasswordGenerator
import mouse.foption.FOptionSyntaxMouse
import telegramium.bots.ChatIntId
import telegramium.bots.Markdown

import java.time.Duration
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.DurationInt
import scala.concurrent.duration.FiniteDuration
import java.time.OffsetDateTime
import telegramium.bots.User
import telegramium.bots.Markdown2
import io.github.oybek.organizer.model.Will
import cats.data.NonEmptyList

trait Hub[F[_]]:
  def duty(offsetDateTime: OffsetDateTime): F[List[Reaction]]
  def handle(chatId: ChatIntId,
             user: User,
             text: String): Context[F[List[Reaction]]]

object Hub:
  def create[F[_]: MonadThrow: Clock, G[_]: Monad](consolePool: PoolManager[F, HldsConsole[F], ChatIntId],
                                                   passwordGenerator: PasswordGenerator[F],
                                                   adminDao: AdminDao[G],
                                                   organizer: Organizer[F],
                                                   tx: G ~> F,
                                                   log: ContextLogger[F]): Hub[F] = 
    new Hub[F]:
      override def duty(offsetDateTime: OffsetDateTime): F[List[Reaction]] =
        organizer.duty(offsetDateTime)

      override def handle(chatId: ChatIntId,
                          user: User,
                          text: String): Context[F[List[Reaction]]] =
        log.info(s"Got message $text") >> {
          CommandParser.parse(text, 2022) match
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
          case WillCommand(hours)      => handleWillCommand(chatId, user, hours)
          case _                       => List(SendText(chatId, "Еще не реализовано"): Reaction).pure[F]

      private def handleWillCommand(chatId: ChatIntId,
                                    user: User,
                                    hours: List[OffsetDateTime]): Context[F[List[Reaction]]] =
        hours match {
          case Nil =>
            List(SendText(
              chatId,
              """
                |If you want to create a will, follow the example:
                |`/will 26.03 19 20 +5`
                |Means that you willing to play on 26th of march
                |at 19 or 20 o clock at UTC+5
                |""".stripMargin,
              Markdown.some
            )).pure[F]
          case h::hs =>
            val hours1 = NonEmptyList.of(h, hs*)
            organizer.save(
              hours1.map { hour =>
                Will(
                  userId = user.id,
                  userName = user.firstName,
                  chatId = chatId.id,
                  hour = hour
                )
              }
            )
        }

      private def handleNewCommand(chatId: ChatIntId, map: Option[String]): Context[F[List[Reaction]]] =

        val caseFind: F[Option[List[Reaction]]] =
          for
            c <- consolePool.find(chatId)
            _ <- c.traverse(_.changeLevel(map.getOrElse("de_dust2"))).void
            r =  c.as(List(SendText(chatId, "You already got the server, just changing a map")))
          yield r

        val caseRent: F[Option[List[Reaction]]] =
          for
            c <- consolePool.rent(chatId)
            r <- c.traverse { console =>
              for
                pass <- passwordGenerator.generate
                _ <- console.svPassword(pass)
                _ <- console.changeLevel(map.getOrElse("de_dust2"))
              yield List(
                SendText(chatId, "Your server is ready. Copy paste this"),
                Sleep(200.millis),
                sendConsole(chatId, console, pass)
              )
            }
          yield r

        val caseNone: List[Reaction] =
          List(SendText(chatId, "No free server left, contact t.me/turtlebots"))

        caseFind.orElseF(caseRent).getOrElse(caseNone)

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
