package io.github.oybek.playcs.setup

import cats.Id
import cats.arrow.FunctionK
import io.github.oybek.playcs.common.Pool
import io.github.oybek.playcs.common.logger.ContextData
import io.github.oybek.playcs.common.logger.ContextLogger
import io.github.oybek.playcs.fakes.*
import io.github.oybek.playcs.client.HldsClient
import io.github.oybek.playcs.service.Hub
import io.github.oybek.playcs.setup.TestEffect.DB
import io.github.oybek.playcs.setup.TestEffect.F
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
  val pool = (List(fakeHlds), List.empty[(Long, HldsClient[F])])
  val passwordGenerator = new FakePasswordGenerator[F]
  val hub: Hub[F] =
    Pool
      .create[F, HldsClient[F]](
        pool,
        hldsConsole =>
          for
            pass <- passwordGenerator.generate
            _ <- hldsConsole.svPassword(pass)
            _ <- hldsConsole.map("de_dust2")
          yield ()
      )
      .map(pool => Hub.create[F](pool, passwordGenerator))
      .toOption
      .get
