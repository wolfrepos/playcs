package io.github.oybek.cstrike.parser

import io.github.oybek.cstrike.model.Command
import io.github.oybek.cstrike.parser.impl.CommandParserImpl

trait CommandParser:
  def parse(text: String, year: Int): String | Command

object CommandParser extends CommandParserImpl
