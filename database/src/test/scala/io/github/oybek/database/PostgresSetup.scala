package io.github.oybek.database

import cats.arrow.FunctionK
import cats.effect.IO
import com.dimafeng.testcontainers.ForAllTestContainer
import com.dimafeng.testcontainers.PostgreSQLContainer
import doobie.ConnectionIO
import doobie.implicits.toConnectionIOOps
import io.github.oybek.database.DbConfig
import org.scalatest.Suite
import org.scalatest.flatspec.AnyFlatSpec
import org.testcontainers.utility.DockerImageName

import scala.concurrent.ExecutionContext.global

trait PostgresSetup extends ForAllTestContainer:
  self: Suite =>

  override val container: PostgreSQLContainer = PostgreSQLContainer(
    DockerImageName.parse("postgres:10.10")
  )

  lazy val transactor = DB.createTransactor[IO](
    DbConfig(
      container.driverClassName,
      container.jdbcUrl,
      container.username,
      container.password
    ),
    global
  )
