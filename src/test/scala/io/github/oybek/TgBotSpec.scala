package io.github.oybek

import org.scalatest.FunSuite

class TgBotSpec extends FunSuite with TgExtractors {

  test("/do command regex") {
    assert("/start" matches "/(start|help).*")
    assert("/help" matches "/(start|help).*")

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
