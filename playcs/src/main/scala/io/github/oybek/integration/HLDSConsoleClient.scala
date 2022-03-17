package io.github.oybek.integration

import cats.effect.{Resource, Sync}
import cats.implicits.catsSyntaxFlatMapOps
import cats.syntax.functor.*
import io.github.oybek.common.time.Timer
import io.github.oybek.integration.HLDSConsoleClient.{InputPusher, OutputPuller}

import java.io.{File, InputStream, OutputStream, PrintWriter}
import java.util.concurrent.ConcurrentLinkedQueue
import scala.concurrent.duration.DurationInt
import scala.io.Source
import scala.sys.process.{Process, ProcessIO}

class HLDSConsoleClient[F[_]: Sync: Timer](val process: Process,
                                           inputPusher: InputPusher,
                                           outputPuller: OutputPuller[F]):
  def execute(s: String): F[Unit] =
    Sync[F].delay(inputPusher.push(s)) >> Timer[F].sleep(200.millis)
  def readln: F[Option[String]] = outputPuller.pull

object HLDSConsoleClient:
  def create[F[_]: Sync: Timer](port: Int, hldsDir: File): Resource[F, HLDSConsoleClient[F]] =
    val processDesc = Process(
      s"./hlds_run -game cstrike +ip 0.0.0.0 +port $port +maxplayers 12 +map de_dust2 +exec server.cfg",
      hldsDir
    )
    val inputPusher = new InputPusher
    val outputPuller = new OutputPuller[F]
    val processIO = new ProcessIO(inputPusher.pusher, outputPuller.puller, _ => ())
    Resource.make(
      Sync[F].delay(processDesc.run(processIO)).map(
        new HLDSConsoleClient[F](_, inputPusher, outputPuller)
      )
    )(consoleLow => Sync[F].delay(consoleLow.process.destroy()))

  class OutputPuller[F[_]: Sync]:
    private val queue = new ConcurrentLinkedQueue[String]()
    def puller(is: InputStream): Unit = {
      Source
        .fromInputStream(is)
        .getLines()
        .foreach(queue.add)
    }
    def pull: F[Option[String]] =
      Sync[F].delay(Option(queue.poll()))

  class InputPusher:
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
