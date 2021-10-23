package io.github.oybek.cstrike

import cats.implicits.catsSyntaxEitherId
import io.github.oybek.cstrike.model.Command
import io.github.oybek.cstrike.model.Command.{HelpCommand, JoinCommand, MapsCommand, NewCommand, StatusCommand}
import io.github.oybek.cstrike.service.impl.TranslatorImpl
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers._
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime

class TranslatorSpec extends AnyFlatSpec {

  private val tests = Table(
    ("text", "command"),
    ("/new@playcs_bot de_dust2 40"     , NewCommand("de_dust2", 40.minutes).asRight[String]),
    (" /new@playcs_bot  de_dust2   40 ", NewCommand("de_dust2", 40.minutes).asRight[String]),
    ("/new@playcs_bot de_dust2"        , NewCommand("de_dust2", 60.minutes).asRight[String]),
    ("/new@playcs_bot"                 , NewCommand("de_dust2", 60.minutes).asRight[String]),
    ("/new"                            , NewCommand("de_dust2", 60.minutes).asRight[String]),
    ("/new@playcs_bot de_dust2 hello"  , "endOfInput".asLeft[Command]),
    ("/new de_dust2 40"                , NewCommand("de_dust2", 40.minutes).asRight[String]),
    (" /new  de_dust2   40 "           , NewCommand("de_dust2", 40.minutes).asRight[String]),
    ("/new de_dust2"                   , NewCommand("de_dust2", 60.minutes).asRight[String]),
    ("/new de_dust2 hello"             , "endOfInput".asLeft[Command]),
    ("/join"                           , JoinCommand.asRight[String]),
    ("   /join@playcs_bot   "          , JoinCommand.asRight[String]),
    ("/help"                           , HelpCommand.asRight[String]),
    ("   /help@playcs_bot   "          , HelpCommand.asRight[String]),
    ("/status"                         , StatusCommand.asRight[String]),
    ("   /status@playcs_bot   "        , StatusCommand.asRight[String]),
    ("/maps"                           , MapsCommand.asRight[String]),
    ("   /maps@playcs_bot   "          , MapsCommand.asRight[String]),
    ("   /map@playcs_bot   "           , "Unknown command".asLeft[Command]),
  )

  "translator" should "be tested" in {
    forAll(tests) {
      (text, command) => translator.translate(text) shouldEqual command
    }
  }

  private lazy val translator = new TranslatorImpl
}
