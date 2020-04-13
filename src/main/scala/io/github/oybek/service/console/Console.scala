package io.github.oybek.service.console

import java.io.{InputStream, OutputStream, PrintWriter}
import java.util.concurrent.ConcurrentLinkedQueue

import cats.effect.Sync
import cats.syntax.all._
import io.github.oybek.domain.Cmd

import scala.io.Source
import scala.sys.process.{Process, ProcessIO}

class Console[F[_]: Sync](process: Process,
                          inputPusher: InputPusher,
                          outputPuller: OutputPuller[F],
                          val mapp: String) extends ConsoleAlg[F] {

  def println(s: String): F[Unit] = Sync[F].delay(inputPusher.push(s))
  def readln: F[Option[String]] = outputPuller.pull
}

object Console {
  def runProcess[F[_]: Sync](cmd: Cmd): F[Console[F]] = {
    val processDesc = Process(cmd.expr, cmd.workDir)
    val inputPusher = new InputPusher
    val outputPuller = new OutputPuller[F]
    for {
      processIO <- Sync[F].delay { new ProcessIO(inputPusher.pusher, outputPuller.puller, _ => ()) }
      process <- Sync[F].delay { processDesc.run(processIO) }
    } yield
      new Console[F](
        process,
        inputPusher,
        outputPuller,
        cmd.args.find(_._1 == "+map").map(_._2).getOrElse("unknown")
      )
  }
}

private class OutputPuller[F[_]: Sync] {

  private val queue = new ConcurrentLinkedQueue[String]()

  def puller(is: InputStream): Unit = {
    Source
      .fromInputStream(is)
      .getLines
      .foreach(queue.add)
  }

  def pull: F[Option[String]] =
    Sync[F].delay(Option(queue.poll()))
}

private class InputPusher {

  private val queue = new ConcurrentLinkedQueue[String]()

  def pusher(os: OutputStream): Unit = synchronized {
    val pw = new PrintWriter(os)
    while (true) {
      wait()
      Option(queue.poll()).foreach { s =>
        pw.println(s)
        pw.flush()
      }
    }
  }

  def push(s: String): Unit = synchronized {
    queue.add(s)
    notify()
  }
}
