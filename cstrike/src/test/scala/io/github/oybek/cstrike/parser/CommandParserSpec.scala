package io.github.oybek.cstrike.parser

import cats.implicits.*
import io.github.oybek.cstrike.model.Command
import io.github.oybek.cstrike.model.Command.*
import io.github.oybek.cstrike.parser.impl.CommandParserImpl
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers.*
import org.scalatest.prop.TableDrivenPropertyChecks.*
import org.scalatest.prop.TableFor2

class CommandParserSpec extends AnyFlatSpec:

  val tests: TableFor2[String, String | Command] = Table(
    ("String", "Command"),
    ("/new@playcs_bot de_dust2"       , NewCommand("de_dust2".some)),
    (" /new@playcs_bot  de_dust2"     , NewCommand("de_dust2".some)),
    ("/new@playcs_bot de_dust2"       , NewCommand("de_dust2".some)),
    ("/new@playcs_bot"                , NewCommand(None)),
    ("/new"                           , NewCommand(None)),
    ("/new@playcs_bot de_dust2 hello" , "endOfInput"),
    ("/new de_dust2"                  , NewCommand("de_dust2".some)),
    (" /new  de_dust2 "               , NewCommand("de_dust2".some)),
    ("/new de_dust2"                  , NewCommand("de_dust2".some)),
    ("/new de_dust2 hello"            , "endOfInput"),
    ("/join"                          , JoinCommand),
    ("   /join@playcs_bot   "         , JoinCommand),
    ("/help"                          , HelpCommand),
    ("   /help@playcs_bot   "         , HelpCommand),
    ("/balance"                       , BalanceCommand),
    ("   /balance@playcs_bot   "      , BalanceCommand),
    ("/maps"                          , MapsCommand),
    ("   /maps@playcs_bot   "         , MapsCommand),
    ("   /map@playcs_bot   "          , "Unknown command"),
  )

  "translator".should("be tested") in {
    forAll(tests) {
      (text, command) => assert(translator.parse(text) === command)
    }
  }

  private lazy val translator = new CommandParserImpl
