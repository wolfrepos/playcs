package io.github.oybek

import java.io.PrintWriter
import java.util.concurrent.ConcurrentLinkedQueue

import cats.syntax.all._
import cats.effect._

import scala.io.Source
import scala.sys.process.{Process, ProcessIO}

object PlayCS extends IOApp {

  type F[+T] = IO[T]

  def run(args: List[String]): IO[ExitCode] = {
    val process = Process("calc")
    val octopus = new ProcessIO(
      os => {
        val pw = new PrintWriter(os)
        while (true) {
          Thread.sleep(2000)
          pw.println("2+2")
          pw.flush()
        }
      },
      stdout => Source.fromInputStream(stdout).getLines.foreach(println),
      stderr => Source.fromInputStream(stderr).getLines.foreach(println)
    )
    Sync[F].delay(process.run(octopus)).as(ExitCode.Success)
  }

}
