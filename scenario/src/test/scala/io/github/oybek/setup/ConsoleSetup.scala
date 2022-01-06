package io.github.oybek.setup

import cats.arrow.FunctionK
import io.github.oybek.fakes._
import io.github.oybek.model.ConsolePool
import io.github.oybek.service.impl.{ConsoleImpl, HldsConsolePoolManagerImpl}
import io.github.oybek.setup.TestEffect.{DB, F}

trait ConsoleSetup {
  val clock              = new FakeClock[F]
  implicit val fakeTimer = new FakeTimer[F](clock)
  val hldsConsole        = new FakeHldsConsole[F]
  val consolePool        = ConsolePool[F](free = List(hldsConsole), busy = Nil)
  val consolePoolRef     = new FakeRef[F, ConsolePool[F]](consolePool)
  val logger             = new FakeMessageLogger[F]
  val passwordGen        = new FakePasswordGenerator[F]
  val fakeBalanceDao     = new FakeBalanceDao[DB]
  val transactor         = new FunctionK[DB, F] {
    override def apply[A](fa: DB[A]): F[A] = Right(fa)
  }
  val consolePoolManager = new HldsConsolePoolManagerImpl[F, DB](
    consolePoolRef, passwordGen, fakeBalanceDao, transactor, logger)
  val console            = new ConsoleImpl(consolePoolManager, logger)
}
