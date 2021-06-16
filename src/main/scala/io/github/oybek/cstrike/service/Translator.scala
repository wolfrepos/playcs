package io.github.oybek.cstrike.service

import io.github.oybek.cstrike.model.Command
import io.github.oybek.cstrike.service.impl.TranslatorImpl

trait Translator {
  def translate(text: String): Either[String, Command]
}

object Translator {
  def create: Translator =
    new TranslatorImpl
}
