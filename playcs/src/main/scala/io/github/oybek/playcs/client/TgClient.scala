package io.github.oybek.playcs.client

import cats.Parallel
import cats.effect.Async
import cats.effect.IO
import cats.effect.Temporal
import org.typelevel.log4cats.Logger
import cats.implicits.*
import cats.instances.finiteDuration
import io.github.oybek.playcs.dto.Reaction
import io.github.oybek.playcs.dto.Reaction.*
import io.github.oybek.playcs.service.Bot
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
  def create(api: Api[IO], bot: Bot) =
    new LongPollBot[IO](api):
      override def onMessage(message: Message): IO[Unit] =
        (message.text, message.from)
          .mapN((text, _) =>
            val chatId = ChatIntId(message.chat.id)
            bot
              .handle(chatId, text)
              .recoverWith { th =>
                List(SendText(chatId, s"Something has gone wrong $th"): Reaction).pure[IO]
              }
              .flatMap(interpret)
          )
          .getOrElse(IO.unit)
          .start
          .void

      override def start(): IO[Unit] =
        super.start().void

      private def interpret(reactions: List[Reaction]): IO[Unit] =
        reactions.traverse {
          case SendText(chatId, text, parseMode) =>
            Methods
              .sendMessage(
                chatId = chatId,
                text = text,
                parseMode = parseMode
              )
              .exec(api)
              .void

          case Sleep(finiteDuration) =>
            Temporal[IO].sleep(finiteDuration)

          case Receive(chatId, text) =>
            bot
              .handle(chatId, text)
              .recoverWith { th =>
                List(SendText(chatId, "Something has gone wrong"): Reaction).pure[IO]
              }
              .flatMap(interpret)
        }.void

      private def toNearestHour = IO {
        val now = OffsetDateTime.now
        val nextHour = now.truncatedTo(ChronoUnit.HOURS).plusHours(1)
        val duration = Duration.between(now, nextHour)
        FiniteDuration(duration.getSeconds, TimeUnit.SECONDS)
      }
