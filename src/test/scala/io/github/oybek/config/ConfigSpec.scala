package io.github.oybek.config

import io.github.oybek.database.config.DbConfig
import org.scalatest.funsuite.AnyFunSuite

class ConfigSpec extends AnyFunSuite {

  test("Config load") {
    assert(
      Config.load ==
        Right(Config(
          DbConfig(
            driver = "org.postgresql.Driver",
            url = "url",
            user = "user",
            pass = "pass"
          ),
          "SERVER_IP",
          "BOT_TOKEN",
          "HLDS_DIR",
          2)))
  }
}
