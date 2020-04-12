package io.github.oybek

import telegramium.bots.{Location, Message}

import scala.util.matching.Regex

trait TgExtractors {
  object Location {
    def unapply(msg: Message): Option[Location] =
      msg.location
  }
  object Text {
    def unapply(msg: Message): Option[String] =
      msg.text
  }

  val `/new`: Regex = "/new ([a-z_]+) ([0-9][hm])".r
}
