package io.github.oybek.integration

import cats.effect.{Resource, Sync}
import cats.implicits.catsSyntaxFlatMapOps
import cats.syntax.functor.*
import io.github.oybek.common.time.Timer
import io.github.oybek.integration.HLDSConsoleClient.InputPusher

import java.io.{File, InputStream, OutputStream, PrintWriter}
import java.util.concurrent.ConcurrentLinkedQueue
import scala.concurrent.duration.DurationInt
import scala.io.Source
import scala.sys.process.{Process, ProcessIO}
import java.io.PipedInputStream

class HLDSConsoleClient[F[_]: Sync: Timer](val process: Process,
                                           inputPusher: InputPusher):
  def execute(s: String): F[Unit] =
    Sync[F].delay(inputPusher.push(s)) >> Timer[F].sleep(200.millis)

object HLDSConsoleClient:
  def create[F[_]: Sync: Timer](port: Int, hldsDir: File): Resource[F, HLDSConsoleClient[F]] =
    val processDesc = Process(
      s"./hlds_run -game cstrike +ip 0.0.0.0 +port $port +maxplayers 12 +map de_dust2 +exec server.cfg",
      hldsDir
    )
    val inputPusher = new InputPusher
    val processIO = new ProcessIO(inputPusher.pusher, _ => (), _ => ())
    Resource.make(
      Sync[F].delay(processDesc.run(processIO)).map(
        new HLDSConsoleClient[F](_, inputPusher)
      )
    )(consoleLow => Sync[F].delay(consoleLow.process.destroy()))

  class InputPusher:
    private val queue = new ConcurrentLinkedQueue[String]()

    def pusher(os: OutputStream): Unit = synchronized {
      while (true) {
        wait()
        Option(queue.poll()).foreach { s =>
          os.write((s + System.lineSeparator()).getBytes)
          os.flush()
        }
      }
    }

    def push(s: String): Unit = {
      queue.add(s)
      notify()
    }
