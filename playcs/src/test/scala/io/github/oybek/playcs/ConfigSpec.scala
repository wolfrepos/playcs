package io.github.oybek.playcs

import cats.data.NonEmptyList
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.scalatest.funsuite.AnyFunSuite
import scala.concurrent.duration.DurationInt

import java.util.Properties

class ConfigSpec extends AnyFunSuite:

  test("Config load") {
    val properties = List(
      "server.ip" -> "SERVER_IP",
      "tg.token" -> "BOT_TOKEN",
      "hlds.dir" -> "HLDS_DIR",
      "pool.size" -> "2",
      "hlds.timeout" -> "2"
    )
    properties.map { case (key, value) =>
      System.setProperty(key, value)
    }

    val loadedConfig = Config.create[IO].attempt.unsafeRunSync()
    val expectedConfig = Right(
      Config(
        "SERVER_IP",
        "BOT_TOKEN",
        "HLDS_DIR",
        2,
        2.minutes
      )
    )
    assert(loadedConfig === expectedConfig)
  }
