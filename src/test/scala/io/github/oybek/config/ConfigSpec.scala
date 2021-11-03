package io.github.oybek.config

import org.scalatest.funsuite.AnyFunSuite

class ConfigSpec extends AnyFunSuite {

  test("Config load") {
    assert(
      Config.load ==
        Right(Config(
          "SERVER_IP",
          "BOT_TOKEN",
          "HLDS_DIR",
          2
        ))
    )
  }
}
