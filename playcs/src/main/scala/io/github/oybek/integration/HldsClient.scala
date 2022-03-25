package io.github.oybek.integration

import cats.effect.IO
import cats.effect.{Resource, Sync}
import cats.implicits.catsSyntaxFlatMapOps
import cats.syntax.functor.*
import io.github.oybek.common.time.Timer

import java.io.{File, InputStream, OutputStream, PrintWriter}
import java.util.concurrent.ConcurrentLinkedQueue
import scala.concurrent.duration.DurationInt
import scala.io.Source
import scala.sys.process.{Process, ProcessIO}
import java.io.PipedInputStream

trait HldsClient[F[_]]:
  def execute(command: String): F[Unit]
  def process: Process

object HldsClient:
  def create(port: Int, hldsDir: File): Resource[IO, HldsClient[IO]] =
    val processDesc = Process(
      s"./hlds_run -game cstrike +ip 0.0.0.0 +port $port +maxplayers 12 +map de_dust2 +exec server.cfg",
      hldsDir
    )
    val gate = new Gate
    val processIO = new ProcessIO(gate.stream, _ => (), _ => ())
    Resource.make {
      IO(processDesc.run(processIO)).map { p =>
        new HldsClient[IO]:
          override val process = p
          def execute(s: String): IO[Unit] =
            IO(gate.push(s)) >> IO.sleep(200.millis)
      }
    } { consoleLow =>
      IO.delay(consoleLow.process.destroy())
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
