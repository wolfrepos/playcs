package io.github.oybek.database.organizer.dao

import cats.effect.unsafe.implicits.global
import doobie.*
import doobie.implicits.*
import io.github.oybek.database.DB
import io.github.oybek.database.PostgresSetup
import io.github.oybek.organizer.model.Will
import io.github.oybek.organizer.service.OrganizerDao

import java.time.OffsetDateTime
import java.time.ZoneOffset
import scala.concurrent.ExecutionContext.global as globalEc
import scala.concurrent.duration.DurationInt
import com.dimafeng.testcontainers.scalatest.TestContainerForAll
import org.scalatest.funsuite.AnyFunSuite

trait OrganizerDaoSpec extends AnyFunSuite with PostgresSetup:

  val organizerDao = OrganizerDao.create

  val someTime = OffsetDateTime.of(2022, 3, 25, 9, 0, 0, 0, ZoneOffset.ofHours(5))
  val will = Will(
    chatId = 123,
    userId = 123,
    start  = someTime,
    end    = someTime.plusHours(1)
  )

  test("OrganizerDao") {
    //transactor.use { tx =>
    //}.unsafeRunTimed(10.seconds)
  } 