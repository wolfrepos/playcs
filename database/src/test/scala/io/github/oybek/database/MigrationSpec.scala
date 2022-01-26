package io.github.oybek.database

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.dimafeng.testcontainers.{ForAllTestContainer, PostgreSQLContainer}
import io.github.oybek.database.config.DbConfig
import org.scalatest.flatspec.AnyFlatSpec
import scala.concurrent.ExecutionContext.{global => globalEc}

class MigrationSpec extends AnyFlatSpec with ForAllTestContainer  {

  override val container: PostgreSQLContainer = PostgreSQLContainer("postgres:10.10")

  "db/migration" should "be successful" in {
    val transactor = DB.createTransactor[IO](
      DbConfig(
        container.driverClassName,
        container.jdbcUrl,
        container.username,
        container.password
      ),
      globalEc
    )

    transactor.use(DB.runMigrations(_)).unsafeRunSync()
  }
}

