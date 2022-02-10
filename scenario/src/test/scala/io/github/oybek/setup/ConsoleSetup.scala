package io.github.oybek.setup

import cats.Id
import cats.arrow.FunctionK
import io.github.oybek.common.logger.{ContextData, ContextLogger}
import io.github.oybek.common.time.{Clock, Timer}
import io.github.oybek.fakes.*
import io.github.oybek.model.ConsolePool
import io.github.oybek.service.Console
import io.github.oybek.service.impl.{ConsoleImpl, HldsConsolePoolManagerImpl}
import io.github.oybek.setup.TestEffect.{DB, F}

trait ConsoleSetup:
  given ContextData(1234)
  given fakeTimer: Timer[F] = new FakeTimer[F]
  given fakeClock: Clock[F] = new FakeClock[F]
  val hldsConsole           = new FakeHldsConsole[F]
  val consolePool           = ConsolePool[F](free = List(hldsConsole), busy = Nil)
  val consolePoolRef        = new FakeRef[F, ConsolePool[F]](consolePool)
  val logger                = new ContextLogger[F](new FakeMessageLogger[F])
  val passwordGen           = new FakePasswordGenerator[F]
  val fakeBalanceDao        = new FakeBalanceDao[DB]
  val transactor            = new FunctionK[DB, F] {
    override def apply[A](fa: DB[A]): F[A] = Right(fa)
  }
  val consolePoolManager = new HldsConsolePoolManagerImpl[F, DB](consolePoolRef, passwordGen, logger)
  def setupConsole: Console[F] = new ConsoleImpl(consolePoolManager, fakeBalanceDao, transactor, logger)
  val console: Console[F] = new ConsoleImpl(consolePoolManager, fakeBalanceDao, transactor, logger)
