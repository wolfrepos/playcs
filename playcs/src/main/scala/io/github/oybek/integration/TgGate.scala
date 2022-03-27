package io.github.oybek.integration

import cats.Parallel
import cats.effect.Async
import cats.effect.IO
import cats.effect.Temporal
import cats.implicits.*
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

import java.time.OffsetDateTime
import scala.concurrent.duration.FiniteDuration

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

      def every(duration: FiniteDuration): IO[Unit] =
        given ContextData(7777)
        fs2.Stream
          .awakeEvery[IO](duration)
          .foreach { _ =>
            for
              now       <- IO { OffsetDateTime.now }
              reactions <- console.duty(now)
              _         <- interpret(reactions)
            yield ()
          }
          .compile
          .drain

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
