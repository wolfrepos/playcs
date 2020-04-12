package io.github.oybek

import java.io.File

import cats.syntax.all._
import cats.effect.Sync

import scala.sys.process.{Process, ProcessIO}

class Octopus[F[_]: Sync](process: Process,
                          inputPusher: InputPusher,
                          outputPuller: OutputPuller[F]) {

  def isAlive: F[Boolean] = Sync[F].delay(process.isAlive())
  def destroy: F[Unit] = Sync[F].delay(process.destroy())
  def push(s: String): F[Unit] = Sync[F].delay(inputPusher.push(s))
  def pull: F[Option[String]] = outputPuller.pull
}

object Octopus {
  def run[F[_]: Sync](cmd: String, cwd: File): F[Octopus[F]] = {
    val processDesc = Process(
      "./hlds_run -console -game cstrike +ip 0.0.0.0 +maxplayers 12 +map cs_mansion",
      new java.io.File("/home/oybek/Garage/SteamCMD/hlds"))
    val inputPusher = new InputPusher
    val outputPuller = new OutputPuller[F]
    for {
      processIO <- Sync[F].delay { new ProcessIO(inputPusher.pusher, outputPuller.puller, _ => ()) }
      process <- Sync[F].delay { processDesc.run(processIO) }
    } yield new Octopus[F](process, inputPusher, outputPuller)
  }
}
