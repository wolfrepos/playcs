package io.github.oybek

import cats.instances.option._
import cats.syntax.all._
import cats.effect._
import io.github.oybek.domain.CmdStartCSDS
import io.github.oybek.service.Octopus

import scala.concurrent.duration._
import java.io.File

object PlayCS extends IOApp {

  type F[+T] = IO[T]

  def pull(octo: Octopus[F]): F[Unit] =
    for {
      so <- octo.pull
      _ <- so.traverse(x => Sync[F].delay { println(x) })
      _ <- Timer[F].sleep(1 second)
      _ <- pull(octo)
    } yield ()

  def run(args: List[String]): IO[ExitCode] = {
    for {
      octopus <- Octopus.run[F](
        CmdStartCSDS(new File("/home/oybek/Garage/SteamCMD/hlds"))("cs_mansion", 27014)
      )
      _ <- (
        Timer[F].sleep(20 second) *>
          octopus.push("sv_gravity 100")
      ).start.void
      _ <- pull(octopus).start
    } yield ExitCode.Success
  }

}
