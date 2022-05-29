package io.github.oybek.cstrike.parser

import cats.implicits.*
import io.github.oybek.cstrike.model.Command
import io.github.oybek.cstrike.model.Command.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers.*
import org.scalatest.prop.TableDrivenPropertyChecks.*
import org.scalatest.prop.TableFor2
import java.time.OffsetDateTime

import scala.concurrent.duration.DurationLong
import org.scalactic.Equality

class CommandParserSpec extends AnyFlatSpec:

  val tests: TableFor2[String, String | Command] = Table(
    ("String", "Command"),
    ("/new@playcs_bot de_dust2"         , NewCommand("de_dust2".some)),
    (" /new@playcs_bot  de_dust2"       , NewCommand("de_dust2".some)),
    ("/new@playcs_bot de_dust2"         , NewCommand("de_dust2".some)),
    ("/new@playcs_bot"                  , NewCommand(None)),
    ("/new"                             , NewCommand(None)),
    ("/new@playcs_bot de_dust2 hello"   , "endOfInput"),
    ("/new de_dust2"                    , NewCommand("de_dust2".some)),
    (" /new  de_dust2 "                 , NewCommand("de_dust2".some)),
    ("/new de_dust2"                    , NewCommand("de_dust2".some)),
    ("/new de_dust2 hello"              , "endOfInput"),
    ("/help"                            , HelpCommand),
    ("   /help@playcs_bot   "           , HelpCommand),
    ("/balance"                         , BalanceCommand),
    ("   /balance@playcs_bot   "        , BalanceCommand),
    ("   /map@playcs_bot   "            , "Unknown command"),
  )

  "translator".should("be tested") in {
    forAll(tests) {
      (text, command) => assert(CommandParser.parse(text) === command)
    }
  }
