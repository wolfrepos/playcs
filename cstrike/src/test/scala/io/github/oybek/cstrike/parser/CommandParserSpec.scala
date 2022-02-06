package io.github.oybek.cstrike.parser

import cats.implicits.catsSyntaxEitherId
import io.github.oybek.cstrike.model.Command
import io.github.oybek.cstrike.model.Command._
import io.github.oybek.cstrike.parser.impl.CommandParserImpl
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers._
import org.scalatest.prop.TableDrivenPropertyChecks._

class CommandParserSpec extends AnyFlatSpec {

  private val tests = Table(
    ("String", "Command"),
    ("/new@playcs_bot de_dust2"       , NewCommand("de_dust2").asRight[String]),
    (" /new@playcs_bot  de_dust2"     , NewCommand("de_dust2").asRight[String]),
    ("/new@playcs_bot de_dust2"       , NewCommand("de_dust2").asRight[String]),
    ("/new@playcs_bot"                , NewCommand("de_dust2").asRight[String]),
    ("/new"                           , NewCommand("de_dust2").asRight[String]),
    ("/new@playcs_bot de_dust2 hello" , "endOfInput".asLeft[Command]),
    ("/new de_dust2"                  , NewCommand("de_dust2").asRight[String]),
    (" /new  de_dust2 "               , NewCommand("de_dust2").asRight[String]),
    ("/new de_dust2"                  , NewCommand("de_dust2").asRight[String]),
    ("/new de_dust2 hello"            , "endOfInput".asLeft[Command]),
    ("/join"                          , JoinCommand.asRight[String]),
    ("   /join@playcs_bot   "         , JoinCommand.asRight[String]),
    ("/help"                          , HelpCommand.asRight[String]),
    ("   /help@playcs_bot   "         , HelpCommand.asRight[String]),
    ("/balance"                       , BalanceCommand.asRight[String]),
    ("   /balance@playcs_bot   "      , BalanceCommand.asRight[String]),
    ("/maps"                          , MapsCommand.asRight[String]),
    ("   /maps@playcs_bot   "         , MapsCommand.asRight[String]),
    ("   /map@playcs_bot   "          , "Unknown command".asLeft[Command]),
  )

  "translator" should "be tested" in {
    forAll(tests) {
      (text, command) => assert(translator.parse(text) === command)
    }
  }

  private lazy val translator = new CommandParserImpl
}
