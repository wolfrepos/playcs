package io.github.oybek.database

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.dimafeng.testcontainers.{ForAllTestContainer, PostgreSQLContainer}
import io.github.oybek.database.config.DbConfig
import org.testcontainers.utility.DockerImageName

import scala.concurrent.ExecutionContext.global as globalEc
import org.scalatest.funsuite.AnyFunSuite

trait MigrationSpec extends AnyFunSuite with PostgresSetup:

  test("Migrations") {
    transactor.use(DB.runMigrations(_)).unsafeRunSync()
  }

