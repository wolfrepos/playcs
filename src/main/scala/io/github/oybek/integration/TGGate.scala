package io.github.oybek.integration

import cats.Parallel
import cats.effect.{Async, Temporal}
import cats.implicits.{catsSyntaxApplicativeError, catsSyntaxApplicativeId, toFlatMapOps, toFunctorOps, toTraverseOps}
import io.github.oybek.exception.BusinessException
import io.github.oybek.model.Reaction
import io.github.oybek.model.Reaction.{SendText, Sleep}
import io.github.oybek.service.Console
import org.typelevel.log4cats.MessageLogger
import telegramium.bots.high.implicits.methodOps
import telegramium.bots.high.{Api, LongPollBot, Methods}
import telegramium.bots.{ChatIntId, Message}

class TGGate[F[_]: Async: Temporal: Parallel](api: Api[F],
                                              console: Console[F],
                                              logger: MessageLogger[F]) extends LongPollBot[F](api) {

  override def onMessage(message: Message): F[Unit] =
    message
      .text
      .fold(().pure[F])(handle(ChatIntId(message.chat.id), _))

  private def handle(chatId: ChatIntId, text: String): F[Unit] =
    console
      .handle(chatId, text)
      .recoverWith {
        case businessException: BusinessException =>
          businessException.reactions.pure[F]

        case th =>
          logger.info(s"Something went wrong $th").as(
            List(SendText(chatId, "Что-то пошло не так"): Reaction)
          )
      }
      .flatMap(interpret)

  private def interpret(reactions: List[Reaction]): F[Unit] =
    reactions.traverse {
      case SendText(chatId, text, parseMode) =>
        Methods.sendMessage(
          chatId = chatId,
          text = text,
          parseMode = parseMode
        ).exec(api).void

      case Sleep(finiteDuration) =>
        Temporal[F].sleep(finiteDuration)
    }.void
}
