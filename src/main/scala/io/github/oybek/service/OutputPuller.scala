package io.github.oybek.service

import java.io.InputStream
import java.util.concurrent.ConcurrentLinkedQueue

import cats.effect.Sync

import scala.io.Source

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
