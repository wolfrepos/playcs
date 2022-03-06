package io.github.oybek.database

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.dimafeng.testcontainers.{ForAllTestContainer, PostgreSQLContainer}
import io.github.oybek.database.config.DbConfig
import org.scalatest.flatspec.AnyFlatSpec
import org.testcontainers.utility.DockerImageName

import scala.concurrent.ExecutionContext.global as globalEc

class MigrationSpec extends AnyFlatSpec with PostgresSetup:

  "db/migration".should("be successful") in {
    transactor.use(DB.runMigrations(_)).unsafeRunSync()
  }

