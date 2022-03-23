package io.github.oybek.setup

import cats.Id
import cats.arrow.FunctionK
import io.github.oybek.common.PoolManager
import io.github.oybek.common.With
import io.github.oybek.common.logger.ContextData
import io.github.oybek.common.logger.ContextLogger
import io.github.oybek.common.time.Clock
import io.github.oybek.common.time.Timer
import io.github.oybek.fakes.*
import io.github.oybek.service.HldsConsole
import io.github.oybek.service.Hub
import io.github.oybek.setup.TestEffect.DB
import io.github.oybek.setup.TestEffect.F
import telegramium.bots.ChatIntId

trait HubSetup:
  given ContextData(1234)
  given fakeTimer: Timer[F] = new FakeTimer[F]
  given fakeClock: Clock[F] = new FakeClock[F]
  val hldsConsole           = new FakeHldsConsole[F]
  val consolePool           = (List(hldsConsole), Nil)
  val consolePoolRef        = new FakeRef[F, (List[HldsConsole[F]], List[HldsConsole[F] With ChatIntId])](consolePool)
  val logger                = new ContextLogger[F](new FakeMessageLogger[F])
  val passwordGenerator     = new FakePasswordGenerator[F]
  val fakeBalanceDao        = new FakeBalanceDao[DB]
  val fakeAdminDao          = new FakeAdminDao[DB]
  val transactor            = new FunctionK[DB, F]:
    override def apply[A](fa: DB[A]): F[A] = Right(fa)
  val consolePoolManager    = PoolManager.create[F, HldsConsole[F], ChatIntId](
    consolePoolRef,
    hldsConsole =>
      for
        pass <- passwordGenerator.generate
        _ <- hldsConsole.svPassword(pass)
        _ <- hldsConsole.map("de_dust2")
      yield ()
  )
  val hub: Hub[F] = Hub.create[F, DB](consolePoolManager, passwordGenerator, fakeAdminDao, transactor, logger)
