package io.github.oybek.playcs.client

import cats.effect.IO
import cats.effect.Resource
import cats.effect.Sync
import cats.implicits.catsSyntaxFlatMapOps
import cats.syntax.functor.*
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.io.PipedInputStream
import java.io.PrintWriter
import java.util.concurrent.ConcurrentLinkedQueue
import scala.concurrent.duration.DurationInt
import scala.io.Source
import scala.sys.process.Process
import scala.sys.process.ProcessIO

trait HldsDriver:
  def execute(command: String): IO[Unit]
  def process: Process

object HldsDriver:
  def create(port: Int, hldsDir: File): Resource[IO, HldsDriver] =
    val processDesc = Process(
      s"./hlds_run -game cstrike +ip 0.0.0.0 +port $port +maxplayers 12 +map de_dust2 +exec server.cfg",
      hldsDir
    )
    val gate = new Gate
    val processIO = new ProcessIO(gate.stream, _ => (), _ => ())
    Resource.make {
      IO(processDesc.run(processIO)).map { p =>
        new HldsDriver:
          override val process = p
          def execute(s: String): IO[Unit] =
            IO(gate.push(s)) >> IO.sleep(200.millis)
      }
    } { driver =>
      IO.delay(driver.process.destroy())
    }

  private class Gate:
    private val queue = new ConcurrentLinkedQueue[String]()

    def stream(os: OutputStream): Unit = synchronized {
      while (true) {
        wait()
        Option(queue.poll()).foreach { s =>
          os.write((s + System.lineSeparator()).getBytes)
          os.flush()
        }
      }
    }

    def push(s: String): Unit = synchronized {
      queue.add(s)
      notify()
    }
