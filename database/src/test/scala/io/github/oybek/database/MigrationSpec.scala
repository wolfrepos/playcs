package io.github.oybek.database

import cats.effect.{ContextShift, IO}
import com.dimafeng.testcontainers.{ForAllTestContainer, PostgreSQLContainer}
import org.scalatest.flatspec.AnyFlatSpec

import scala.concurrent.ExecutionContext.global

class MigrationSpec extends AnyFlatSpec with ForAllTestContainer  {

  implicit val contextShift: ContextShift[IO] = IO.contextShift(global)
  override val container: PostgreSQLContainer = PostgreSQLContainer()

  "migration" should "be successful" in {
    val transactor = DB.createTransactor[IO](
      container.driverClassName,
      container.jdbcUrl,
      container.username,
      container.password
    )

    DB.runMigrations(transactor).unsafeRunSync()
  }
}

