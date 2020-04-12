package io.github.oybek

import org.scalatest.FunSuite

class TgBotSpec extends FunSuite with TgExtractors {

  test("/new command regex") {
    "/new cs_mansion 1h" match {
      case `/new`(map, time) => assert(map == "cs_mansion" && time == "1h")
      case _ => assert(false)
    }

    "/new de_dust2 30m" match {
      case `/new`(map, time) => assert(map == "de_dust2" && time == "30m")
      case _ => assert(false)
    }

    "/new@playcs_bot de_dust2 30m" match {
      case `/new`(map, time) => assert(map == "de_dust2" && time == "30m")
      case _ => assert(false)
    }

    "/new@ de_dust2 30m" match {
      case `/new`(_, _) => assert(false)
      case _ => assert(true)
    }
  }

  test("/do command regex") {
    "/do say \"hello\"" match {
      case `/do`(cmd) => assert(cmd == "say \"hello\"")
      case _ => assert(false)
    }

    "/do sv_gravity 100" match {
      case `/do`(cmd) => assert(cmd == "sv_gravity 100")
      case _ => assert(false)
    }
  }

}
