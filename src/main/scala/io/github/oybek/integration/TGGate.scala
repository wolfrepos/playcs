package io.github.oybek.integration

import cats.Parallel
import cats.effect.{Async, IO, Temporal}
import cats.implicits.{catsSyntaxApplicativeError, catsSyntaxApplicativeId, toFlatMapOps, toFunctorOps, toTraverseOps}
import io.github.oybek.exception.BusinessException
import io.github.oybek.model.Reaction
import io.github.oybek.model.Reaction.{SendText, Sleep}
import io.github.oybek.service.Console
import org.typelevel.log4cats.MessageLogger
import telegramium.bots.high.implicits.methodOps
import telegramium.bots.high.{Api, LongPollBot, Methods}
import telegramium.bots.{ChatIntId, Message}

class TGGate(api: Api[IO],
             console: Console[IO],
             logger: MessageLogger[IO]) extends LongPollBot[IO](api):

  override def onMessage(message: Message): IO[Unit] =
    message
      .text
      .fold(IO.unit)(handle(ChatIntId(message.chat.id), _))

  private def handle(chatId: ChatIntId, text: String): IO[Unit] =
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

  private def interpret(reactions: List[Reaction]): IO[Unit] =
    reactions.traverse {
      case SendText(chatId, text, parseMode) =>
        Methods.sendMessage(
          chatId = chatId,
          text = text,
          parseMode = parseMode
        ).exec(api).void

      case Sleep(finiteDuration) =>
        Temporal[IO].sleep(finiteDuration)
    }.void
