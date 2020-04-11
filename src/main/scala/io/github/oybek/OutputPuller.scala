package io.github.oybek

import java.io.{InputStream, PrintWriter}
import java.util.concurrent.ConcurrentLinkedQueue

import cats.effect.Sync

import scala.io.Source

class OutputPuller[F[_]: Sync] {

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
