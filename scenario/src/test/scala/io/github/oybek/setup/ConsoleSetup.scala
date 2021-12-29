package io.github.oybek.setup

import io.github.oybek.fakes._
import io.github.oybek.model.ConsolePool
import io.github.oybek.service.impl.{ConsoleImpl, HldsConsolePoolManagerImpl}
import io.github.oybek.setup.TestEffect.F

trait ConsoleSetup {
  val clock                            = new FakeClock[F]
  implicit val fakeTimer               = new FakeTimer[F](clock)
  val hldsConsole                      = new FakeHldsConsole[F]
  val consolePool                      = ConsolePool[F](free = List(hldsConsole), busy = Nil)
  val consolePoolRef                   = new FakeRef[F, ConsolePool[F]](consolePool)
  val logger                           = new FakeMessageLogger[F]
  val passwordGen                      = new FakePasswordGenerator[F]
  val consolePoolManager               = new HldsConsolePoolManagerImpl[F](consolePoolRef, passwordGen, logger)
  val console                          = new ConsoleImpl(consolePoolManager)
}
