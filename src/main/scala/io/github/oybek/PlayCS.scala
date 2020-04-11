package io.github.oybek

import java.io.PrintWriter
import java.util.concurrent.ConcurrentLinkedQueue

import cats.instances.list._
import cats.instances.option._
import cats.syntax.all._
import cats.effect._

import scala.io.Source
import scala.sys.process.{Process, ProcessIO}

import scala.concurrent.duration._

object PlayCS extends IOApp {

  type F[+T] = IO[T]

  def pull(outputPuller: OutputPuller[F]): F[Unit] =
    for {
      so <- outputPuller.pull
      _ <- so.traverse(x => Sync[F].delay { println(x) })
      _ <- Timer[F].sleep(1 second)
      _ <- pull(outputPuller)
    } yield ()

  def run(args: List[String]): IO[ExitCode] = {
    val process = Process("calc")
    val inputPusher = new InputPusher
    val outputPuller = new OutputPuller[F]
    val octopus = new ProcessIO(
      inputPusher.pusher,
      outputPuller.puller,
      _ => ()
    )

    for {
      _ <- Sync[F].delay(process.run(octopus))
      _ <- (1 to 100).toList.traverse { x =>
        Sync[F].delay(inputPusher.push(s"$x + $x")) *>
          Timer[F].sleep(1 second)
      }.start.void
      _ <- pull(outputPuller).start
    } yield ExitCode.Success
  }

}
