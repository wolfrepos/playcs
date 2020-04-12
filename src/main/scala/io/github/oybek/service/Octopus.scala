package io.github.oybek.service

import java.io.File

import cats.effect.Sync
import cats.syntax.all._
import io.github.oybek.domain.Cmd

import scala.sys.process.{Process, ProcessIO}

class Octopus[F[_]: Sync](process: Process,
                          inputPusher: InputPusher,
                          outputPuller: OutputPuller[F],
                          val mapp: String) {

  def isAlive: F[Boolean] = Sync[F].delay(process.isAlive())
  def destroy: F[Unit] = Sync[F].delay(process.destroy())
  def push(s: String): F[Unit] = Sync[F].delay(inputPusher.push(s))
  def pull: F[Option[String]] = outputPuller.pull
}

object Octopus {
  def run[F[_]: Sync](cmd: Cmd): F[Octopus[F]] = {
    val processDesc = Process(cmd.expr, cmd.workDir)
    val inputPusher = new InputPusher
    val outputPuller = new OutputPuller[F]
    for {
      processIO <- Sync[F].delay { new ProcessIO(inputPusher.pusher, outputPuller.puller, _ => ()) }
      process <- Sync[F].delay { processDesc.run(processIO) }
    } yield
      new Octopus[F](
        process,
        inputPusher,
        outputPuller,
        cmd.args.find(_._1 == "+cmd").map(_._2).getOrElse("unknown")
      )
  }
}
