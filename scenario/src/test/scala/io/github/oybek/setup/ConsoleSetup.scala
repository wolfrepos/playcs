package io.github.oybek.setup

import cats.Id
import cats.arrow.FunctionK
import io.github.oybek.common.Pool
import io.github.oybek.common.With
import io.github.oybek.common.logger.ContextData
import io.github.oybek.common.logger.ContextLogger
import io.github.oybek.fakes.*
import io.github.oybek.hlds.Hlds
import io.github.oybek.hub.Hub
import io.github.oybek.setup.TestEffect.DB
import io.github.oybek.setup.TestEffect.F
import telegramium.bots.ChatIntId
import cats.effect.kernel.Ref.Make
import cats.effect.kernel.Ref
import cats.implicits.catsSyntaxApplicativeId

trait HubSetup:
  given ContextData(1234)
  given ContextLogger[F] = new ContextLogger[F](new FakeMessageLogger[F])
  given Make[F] = new Make[F]:
    def refOf[A](a: A): F[Ref[F, A]] =
      (new FakeRef[F, A](a)).pure[F]

  val fakeHlds = new FakeHlds[F]
  val pool = (List(fakeHlds), List.empty[Hlds[F] With Long])
  val passwordGenerator = new FakePasswordGenerator[F]
  val transact = new FunctionK[DB, F]:
    override def apply[A](fa: DB[A]): F[A] = Right(fa)
  val hub: Hub[F] =
    Pool
      .create[F, Long, Hlds[F]](
        pool,
        hldsConsole =>
          for
            pass <- passwordGenerator.generate
            _ <- hldsConsole.svPassword(pass)
            _ <- hldsConsole.map("de_dust2")
          yield ()
      )
      .map(pool => Hub.create[F, DB](pool, passwordGenerator, transact))
      .toOption
      .get
