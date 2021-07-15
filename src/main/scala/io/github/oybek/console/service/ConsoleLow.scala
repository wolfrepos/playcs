package io.github.oybek.console.service

import cats.effect.{Resource, Sync}
import cats.syntax.all._
import io.github.oybek.console.service.impl.ConsoleLowImpl
import io.github.oybek.console.service.impl.ConsoleLowImpl.{InputPusher, OutputPuller}

import java.io.File
import scala.sys.process.{Process, ProcessIO}

trait ConsoleLow[F[_]] {
  def execute(s: String): F[Unit]
  def readln: F[Option[String]]
}

object ConsoleLow {
  def create[F[_]: Sync](port: Int, hldsDir: File): Resource[F, ConsoleLowImpl[F]] = {
    val processDesc = Process(
      s"./hlds_run -game cstrike +ip 0.0.0.0 +port $port +maxplayers 12 +map de_dust2",
      hldsDir
    )
    val inputPusher = new InputPusher
    val outputPuller = new OutputPuller[F]
    val processIO = new ProcessIO(inputPusher.pusher, outputPuller.puller, _ => ())
    Resource.make(
      Sync[F].delay(processDesc.run(processIO)).map(
        new ConsoleLowImpl[F](_, inputPusher, outputPuller)
      )
    )(consoleLow => Sync[F].delay(consoleLow.process.destroy()))
  }
}
