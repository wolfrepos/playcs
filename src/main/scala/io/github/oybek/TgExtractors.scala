package io.github.oybek

import telegramium.bots.{CallbackQuery, Chat, Location, Message, Update, User}

import cats.syntax.all._
import scala.util.matching.Regex

trait TgExtractors {
  object Location {
    def unapply(msg: Message): Option[Location] =
      msg.location
  }
  object Text {
    def unapply(msg: Message): Option[String] =
      msg.text
    def unapply(query: CallbackQuery): Option[String] =
      query.message.flatMap(_.text)
  }
  object Message {
    def unapply(update: Update): Option[(Chat, Message)] =
      update.message.map(x => (x.chat, x))
  }
  object CallbackQuery {
    def unapply(update: Update): Option[(Chat, String, Message)] =
      update.callbackQuery.flatMap(x =>
        for {
          chat <- x.message.map(_.chat)
          text <- x.data
          messageId <- x.message
        } yield (chat, text, messageId)
      )
  }

  val `/do`: Regex = "/do(?:@playcs_bot)? (.*)".r
}
