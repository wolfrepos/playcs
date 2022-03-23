package io.github.oybek.integration

import cats.Parallel
import cats.effect.Async
import cats.effect.IO
import cats.effect.Temporal
import cats.implicits.catsSyntaxApplicativeError
import cats.implicits.catsSyntaxApplicativeId
import cats.implicits.toFlatMapOps
import cats.implicits.toFunctorOps
import cats.implicits.toTraverseOps
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
import telegramium.bots.high.Api
import telegramium.bots.high.LongPollBot
import telegramium.bots.high.Methods
import telegramium.bots.high.implicits.methodOps

object TgGate:
  def create(api: Api[IO],
             console: Hub[IO],
             logger: ContextLogger[IO]) =
    new LongPollBot[IO](api):
      override def onMessage(message: Message): IO[Unit] =
        message
          .text
          .fold(IO.unit)(handle(ChatIntId(message.chat.id), _)(using ContextData(message.chat.id)))

      private def handle(chatId: ChatIntId, text: String): Context[IO[Unit]] =
        console
          .handle(chatId, text)
          .recoverWith {
            case businessException: BusinessException =>
              businessException.reactions.pure[IO]

            case th =>
              logger.info(s"Something went wrong $th").as(
                List(SendText(chatId, "Что-то пошло не так"): Reaction)
              )
          }
          .flatMap(interpret)

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
