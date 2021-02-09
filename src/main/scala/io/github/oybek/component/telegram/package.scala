package io.github.oybek.component

import cats.implicits.catsSyntaxOptionId
import telegramium.bots.{Chat, Message}

package object telegram {

  object Text {
    def unapply(msg: Message): Option[(Chat, String)] =
      msg.text.map((msg.chat, _))
  }

  object `Хуйня` {
    def unapply(msg: Message): Option[Chat] =
      msg.chat.some
  }
}
