package io.github.oybek.integration

import cats.Parallel
import cats.effect.Async
import cats.effect.IO
import cats.effect.Temporal
import cats.implicits.*
import cats.instances.finiteDuration
import io.github.oybek.common.logger.Context
import io.github.oybek.common.logger.ContextData
import io.github.oybek.common.logger.ContextLogger
import io.github.oybek.exception.BusinessException
import io.github.oybek.model.Reaction
import io.github.oybek.model.Reaction.SendText
import io.github.oybek.model.Reaction.Sleep
import io.github.oybek.service.Hub
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

object TgGate:
  def create(api: Api[IO],
             console: Hub[IO],
             logger: ContextLogger[IO]) =
    new LongPollBot[IO](api):
      override def onMessage(message: Message): IO[Unit] =
        given ContextData(message.chat.id)
        (message.text, message.from).mapN(
          (text, user) =>
            val chatId = ChatIntId(message.chat.id)
            console
              .handle(chatId, user, text)
              .recoverWith {
                case businessException: BusinessException =>
                  businessException.reactions.pure[IO]

                case th =>
                  logger.info(s"Something went wrong $th").as(
                    List(SendText(chatId, "Что-то пошло не так"): Reaction)
                  )
              }
              .flatMap(interpret)
        ).getOrElse(IO.unit)

      override def start(): IO[Unit] =
        super.start().both(everyHour).void

      private def everyHour: IO[Unit] =
        import fs2.Stream
        given ContextData(7777)
        val task = 
          for
            now       <- IO { OffsetDateTime.now }
            _         <- logger.info(s"time: $now, duty started")
            reactions <- console.duty(now)
            _         <- interpret(reactions)
          yield ()
        Stream
          .eval(IO(OffsetDateTime.now))
          .map { now =>
            val nextHour = now.truncatedTo(ChronoUnit.HOURS).plusHours(1)
            val duration = Duration.between(now, nextHour)
            FiniteDuration(duration.toSeconds, TimeUnit.SECONDS)
          }
          .flatMap { finiteDuration =>
            Stream.sleep[IO](finiteDuration) ++
            Stream.awakeEvery[IO](1.hour).foreach { _ =>
              task.recoverWith { th =>
                logger.info(s"Something went wrong $th")
              }
            }
          }
          .compile.drain

      private def interpret(reactions: List[Reaction]): Context[IO[Unit]] =
        reactions.traverse {
          case SendText(chatId, text, parseMode) =>
            logger.info(s"Sending message $text") >>
            Methods.sendMessage(
              chatId = chatId,
              text = text,
              parseMode = parseMode
            ).exec(api).void

          case Sleep(finiteDuration) =>
            logger.info(s"Sleeping $finiteDuration") >>
            Temporal[IO].sleep(finiteDuration)
        }.void
