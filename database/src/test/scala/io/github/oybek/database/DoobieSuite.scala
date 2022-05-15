package io.github.oybek.database

import cats.arrow.FunctionK
import cats.effect.IO
import doobie.hikari.HikariTransactor
import com.dimafeng.testcontainers.ForAllTestContainer
import com.dimafeng.testcontainers.PostgreSQLContainer
import doobie.ConnectionIO
import doobie.implicits.toConnectionIOOps
import io.github.oybek.database.DbConfig
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.flatspec.AnyFlatSpec
import org.testcontainers.utility.DockerImageName
import org.flywaydb.core.Flyway

import scala.concurrent.ExecutionContext.global
import doobie.scalatest.IOChecker
import doobie.util.transactor.Transactor

trait DoobieSuite extends ForAllTestContainer:
  this: AnyFunSuite =>

  override val container: PostgreSQLContainer = PostgreSQLContainer(DockerImageName.parse("postgres:10.10"))

  lazy val transactor =
    Transactor.fromDriverManager[IO](
      driver = container.driverClassName,
      url    = container.jdbcUrl,
      user   = container.username,
      pass   = container.password
    )

  override def afterStart(): Unit =
    val flyway = Flyway
      .configure()
      .dataSource(container.jdbcUrl, container.username, container.password)
      .load()
    flyway.migrate()
