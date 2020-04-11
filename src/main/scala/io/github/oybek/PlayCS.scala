package io.github.oybek

import java.io.PrintWriter
import java.util.concurrent.ConcurrentLinkedQueue

import cats.instances.list._
import cats.syntax.all._
import cats.effect._

import scala.io.Source
import scala.sys.process.{Process, ProcessIO}

import scala.concurrent.duration._

object PlayCS extends IOApp {

  type F[+T] = IO[T]

  def run(args: List[String]): IO[ExitCode] = {
    val process = Process("calc")
    val octopus = new ProcessIO(
      InputPuller.puller,
      stdout => Source.fromInputStream(stdout).getLines.foreach(println),
      stderr => Source.fromInputStream(stderr).getLines.foreach(println)
    )

    for {
      _ <- Sync[F].delay(process.run(octopus))
      _ <- (1 to 100).toList.traverse { x =>
        Sync[F].delay(InputPuller.pull(s"$x + $x")) *>
          Timer[F].sleep(1 second)
      }.start.void
    } yield ExitCode.Success
  }

}
