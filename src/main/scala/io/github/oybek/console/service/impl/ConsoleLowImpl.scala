package io.github.oybek.console.service.impl

import cats.effect.Sync
import io.github.oybek.console.service.ConsoleLow
import io.github.oybek.console.service.impl.ConsoleLowImpl.{InputPusher, OutputPuller}

import java.io.{InputStream, OutputStream, PrintWriter}
import java.util.concurrent.ConcurrentLinkedQueue
import scala.io.Source
import scala.sys.process.Process

class ConsoleLowImpl[F[_]: Sync](val process: Process,
                                 inputPusher: InputPusher,
                                 outputPuller: OutputPuller[F]) extends ConsoleLow[F] {
  def execute(s: String): F[Unit] = Sync[F].delay(inputPusher.push(s))
  def readln: F[Option[String]] = outputPuller.pull
}

object ConsoleLowImpl {
  class OutputPuller[F[_]: Sync] {
    private val queue = new ConcurrentLinkedQueue[String]()

    def puller(is: InputStream): Unit = {
      Source
        .fromInputStream(is)
        .getLines()
        .foreach(queue.add)
    }

    def pull: F[Option[String]] =
      Sync[F].delay(Option(queue.poll()))
  }

  class InputPusher {
    private val queue = new ConcurrentLinkedQueue[String]()

    def pusher(os: OutputStream): Unit = synchronized {
      val pw = new PrintWriter(os)
      while (true) {
        wait()
        Option(queue.poll()).foreach { s =>
          // scalastyle:off
          pw.println(s)
          pw.flush()
          // scalastyle:on
        }
      }
    }

    def push(s: String): Unit = synchronized {
      queue.add(s)
      notify()
    }
  }
}
