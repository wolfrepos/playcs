package io.github.oybek.playcs.client

import cats.Parallel
import cats.effect.Async
import cats.effect.IO
import cats.effect.Temporal
import org.typelevel.log4cats.Logger
import cats.implicits.*
import cats.instances.finiteDuration
import io.github.oybek.playcs.common.logger.Context
import io.github.oybek.playcs.common.logger.ContextData
import io.github.oybek.playcs.common.logger.ContextLogger
import io.github.oybek.playcs.dto.Reaction
import io.github.oybek.playcs.dto.Reaction.*
import io.github.oybek.playcs.service.Hub
import telegramium.bots.ChatIntId
import telegramium.bots.Message
import telegramium.bots.User
import telegramium.bots.high.Api
import telegramium.bots.high.LongPollBot
import telegramium.bots.high.Methods
import telegramium.bots.high.implicits.methodOps

import java.time.Duration
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

import concurrent.duration.DurationInt

object TgClient:
  def create(api: Api[IO], console: Hub[IO])(using logger: ContextLogger[IO]) =
    new LongPollBot[IO](api):
      override def onMessage(message: Message): IO[Unit] =
        given ContextData(message.chat.id)
        (message.text, message.from)
          .mapN((text, _) =>
            val chatId = ChatIntId(message.chat.id)
            console
              .handle(chatId, text)
              .recoverWith { th =>
                logger
                  .info(s"Something went wrong $th")
                  .as(List(SendText(chatId, "Что-то пошло не так"): Reaction))
              }
              .flatMap(interpret)
          )
          .getOrElse(IO.unit)
          .start
          .void

      override def start(): IO[Unit] =
        super.start().void

      private def interpret(reactions: List[Reaction]): Context[IO[Unit]] =
        reactions.traverse {
          case SendText(chatId, text, parseMode) =>
            logger.info(s"Sending message $text") >>
              Methods
                .sendMessage(
                  chatId = chatId,
                  text = text,
                  parseMode = parseMode
                )
                .exec(api)
                .void

          case Sleep(finiteDuration) =>
            logger.info(s"Sleeping $finiteDuration") >>
              Temporal[IO].sleep(finiteDuration)

          case Receive(chatId, text) =>
            console
              .handle(chatId, text)
              .recoverWith { th =>
                logger
                  .info(s"Something went wrong $th")
                  .as(List(SendText(chatId, "Что-то пошло не так"): Reaction))
              }
              .flatMap(interpret)
        }.void

      private def toNearestHour = IO {
        val now = OffsetDateTime.now
        val nextHour = now.truncatedTo(ChronoUnit.HOURS).plusHours(1)
        val duration = Duration.between(now, nextHour)
        FiniteDuration(duration.getSeconds, TimeUnit.SECONDS)
      }
