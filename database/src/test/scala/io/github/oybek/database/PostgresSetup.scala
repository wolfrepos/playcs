package io.github.oybek.database

import cats.effect.IO
import com.dimafeng.testcontainers.{ForAllTestContainer, PostgreSQLContainer}
import io.github.oybek.database.config.DbConfig
import org.scalatest.Suite
import org.scalatest.flatspec.AnyFlatSpec
import org.testcontainers.utility.DockerImageName
import scala.concurrent.ExecutionContext.global

trait PostgresSetup extends ForAllTestContainer:
  self: Suite =>

  override val container: PostgreSQLContainer = PostgreSQLContainer(DockerImageName.parse("postgres:10.10"))

  lazy val transactor = DB.createTransactor[IO](
    DbConfig(
      container.driverClassName,
      container.jdbcUrl,
      container.username,
      container.password
    ),
    global
  )
