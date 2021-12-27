package io.github.oybek.database

import cats.effect.{Blocker, ContextShift, IO}
import com.dimafeng.testcontainers.{ForAllTestContainer, PostgreSQLContainer}
import io.github.oybek.database.config.DbConfig
import org.scalatest.flatspec.AnyFlatSpec

import scala.concurrent.ExecutionContext.global

class MigrationSpec extends AnyFlatSpec with ForAllTestContainer  {

  implicit val contextShift: ContextShift[IO] = IO.contextShift(global)
  val blocker: Blocker = Blocker.liftExecutionContext(global)
  override val container: PostgreSQLContainer = PostgreSQLContainer()

  "migration" should "be successful" in {
    val transactor = DB.createTransactor[IO](
      DbConfig(
        container.driverClassName,
        container.jdbcUrl,
        container.username,
        container.password
      ),
      global,
      blocker
    )

    transactor.use(DB.runMigrations(_)).unsafeRunSync()
  }
}

