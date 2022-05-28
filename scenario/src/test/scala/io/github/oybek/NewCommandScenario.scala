package io.github.oybek

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import io.github.oybek.common.With
import io.github.oybek.common.and
import io.github.oybek.fakes.FakeData.anotherFakeChatId
import io.github.oybek.fakes.FakeData.fakeChatId
import io.github.oybek.fakes.FakeData.fakePassword
import io.github.oybek.fakes.FakeData.fakeUser
import io.github.oybek.model.Reaction.SendText
import io.github.oybek.model.Reaction.Sleep
import io.github.oybek.setup.HubSetup
import org.scalatest.GivenWhenThen
import org.scalatest.featurespec.AnyFeatureSpec
import telegramium.bots.Markdown

import java.time.Instant
import scala.concurrent.duration.DurationInt
import org.scalatest.funsuite.AnyFunSuite
import io.github.oybek.database.hlds.model.Hlds

trait NewCommandScenario extends AnyFunSuite with HubSetup:
  test("/new command scenario") {
    (for
      (hub, hldsDao, hlds, runG) <- createHub(hldsNum = 1)
      reaction <- hub.handle(fakeChatId, fakeUser, "/new")
      _ = assert(
        reaction === List(
          SendText(fakeChatId, "Your server is ready. Copy paste this"),
          Sleep(200.millis),
          SendText(fakeChatId, "`connect 127.0.0.1:27015; password 4444`",Some(Markdown))))
      _ = assert(
        hlds.head.getCalledCommands === List(
          s"sv_password $fakePassword",
          "map de_dust2",
          s"sv_password $fakePassword",
          "changelevel de_dust2"))
      dbEntries <- runG(hldsDao.all)
      _ = assert(
        dbEntries === List(
          Hlds(
            fakeChatId.id,
            fakePassword,
            "de_dust2")))

      _ = hlds.head.reset

      reaction <- hub.handle(fakeChatId, fakeUser, "/new")
      _ = assert(
        reaction === List(
          SendText(fakeChatId, "You already have the server, just changing a map")))
      _ = assert(
        hlds.head.getCalledCommands === List(
          "changelevel de_dust2"))
      dbEntries <- runG(hldsDao.all)
      _ = assert(
        dbEntries === List(
          Hlds(
            fakeChatId.id,
            fakePassword,
            "de_dust2")))
    yield ()).unsafeRunSync()
  }