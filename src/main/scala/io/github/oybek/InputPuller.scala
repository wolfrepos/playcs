package io.github.oybek

import java.io.{OutputStream, PrintWriter}
import java.util.concurrent.ConcurrentLinkedQueue

object InputPuller {

  private val queue = new ConcurrentLinkedQueue[String]()

  def puller(os: OutputStream): Unit = synchronized {
    val pw = new PrintWriter(os)
    while (true) {
      wait()
      Option(queue.poll()).foreach { s =>
        pw.println(s)
        pw.flush()
      }
    }
  }

  def pull(s: String): Unit = synchronized {
    queue.add(s)
    notify()
  }
}
