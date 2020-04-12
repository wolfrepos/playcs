package io.github.oybek

import org.scalatest.FunSuite

class TgBotSpec extends FunSuite with TgExtractors {

  test("Regex") {
    "/new cs_mansion 1h" match {
      case `/new`(map, time) => assert(map == "cs_mansion" && time == "1h")
      case _ => assert(false)
    }
  }
}
