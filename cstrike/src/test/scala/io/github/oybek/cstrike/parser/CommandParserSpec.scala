package io.github.oybek.cstrike.parser

import cats.implicits.*
import io.github.oybek.cstrike.model.Command
import io.github.oybek.cstrike.model.Command.*
import io.github.oybek.cstrike.parser.impl.CommandParserImpl
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
    ("/balance 123 32"                  , IncreaseBalanceCommand(123, 32.minutes)),
    ("/balance@playcs_bot 12 3"         , IncreaseBalanceCommand(12, 3.minutes)),
    ("   /say@playcs_bot hello"         , SayCommand("hello")),
    ("   /say hello"                    , SayCommand("hello")),
    (" /will@playcs_bot "               , WillCommand(Nil)),
    ("/will"                            , WillCommand(Nil)),
    (" /will@playcs_bot 26.03 19 20 +5" , {
      val h1 = OffsetDateTime.parse("2022-03-26T19:00:00+05:00")
      val h2 = OffsetDateTime.parse("2022-03-26T20:00:00+05:00")
      WillCommand(List(h1, h2))
    }),
    ("   /map@playcs_bot   "            , "Unknown command"),
  )

  "translator".should("be tested") in {
    forAll(tests) {
      (text, command) => assert(translator.parse(text, 2022) === command)
    }
  }

  private lazy val translator = new CommandParserImpl
