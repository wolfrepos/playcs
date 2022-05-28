package io.github.oybek.setup

import cats.arrow.FunctionK
import cats.~>
import cats.effect.IO
import cats.effect.kernel.Ref
import cats.effect.kernel.Resource
import cats.effect.kernel.syntax.resource
import cats.implicits.toFlatMapOps
import cats.syntax.contravariant
import com.dimafeng.testcontainers.ForAllTestContainer
import com.dimafeng.testcontainers.PostgreSQLContainer
import doobie.ConnectionIO
import doobie.WeakAsync
import doobie.implicits.toConnectionIOOps
import doobie.util.ExecutionContexts
import io.github.oybek.common.PoolManager
import io.github.oybek.common.With
import io.github.oybek.common.logger.ContextData
import io.github.oybek.common.logger.ContextLogger
import io.github.oybek.database.DB
import io.github.oybek.database.DbConfig
import io.github.oybek.database.hlds.dao.HldsDao
import io.github.oybek.fakes.*
import io.github.oybek.hlds.HldsConsole
import io.github.oybek.hub.Hub
import io.github.oybek.password.PasswordGenerator
import org.testcontainers.utility.DockerImageName
import telegramium.bots.ChatIntId
import org.scalatest.funsuite.AnyFunSuite
import doobie.util.transactor.Transactor

trait HubSetup extends ForAllTestContainer:
  this: AnyFunSuite =>

  given ContextData(1L)

  override val container: PostgreSQLContainer = PostgreSQLContainer(DockerImageName.parse("postgres:10.10"))

  lazy val transactor =
    Transactor.fromDriverManager[IO](
      driver = container.driverClassName,
      url    = container.jdbcUrl,
      user   = container.username,
      pass   = container.password
    )

  lazy val resources =
    for
      tranEc <- ExecutionContexts.cachedThreadPool[IO]
      transactor <- DB.createTransactor[IO](
        DbConfig(
          driver = container.driverClassName,
          url    = container.jdbcUrl,
          user   = container.username,
          pass   = container.password
        ),
        tranEc
      )
      weakAsync <- WeakAsync.liftK[IO, ConnectionIO]
    yield (transactor, weakAsync)

  val passwordGenerator = PasswordGenerator.fake[IO]("4444")
  def createHub(hldsNum: Int): IO[(Hub[IO],
                                   HldsDao[ConnectionIO],
                                   List[FakeHldsConsole[IO]],
                                   ConnectionIO ~> IO)] = resources.use {
    (transactor0, weakAsync) =>
      val consoles = List(new FakeHldsConsole[IO])
      val consolePool = (consoles, Nil)
      val hldsDao = HldsDao.create
      val runG = new FunctionK[ConnectionIO, IO]:
        override def apply[A](a: ConnectionIO[A]): IO[A] =
          a.transact(transactor)
      for
        contextLogger <- ContextLogger.create[IO]
        given ContextLogger[IO] = contextLogger
        _ <- DB.runMigrations[IO](transactor0)
        consolePoolRef <- Ref.of[IO, (List[HldsConsole[IO]], List[HldsConsole[IO] With ChatIntId])](consolePool)
        consolePoolManager = PoolManager.create[IO, HldsConsole[IO], ChatIntId](
          consolePoolRef,
          hldsConsole =>
            for
              pass <- passwordGenerator.generate
              _ <- hldsConsole.svPassword(pass)
              _ <- hldsConsole.map("de_dust2")
            yield ()
        )
        hub = Hub.create[IO, ConnectionIO](
          consolePoolManager,
          passwordGenerator,
          hldsDao,
          runG)
      yield (hub, hldsDao, consoles, runG)
  }
