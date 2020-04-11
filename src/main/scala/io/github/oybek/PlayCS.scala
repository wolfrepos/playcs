package io.github.oybek

import cats.syntax.all._
import cats.effect._

object PlayCS extends IOApp {

  type F[+T] = IO[T]

  def run(args: List[String]): IO[ExitCode] =
    Sync[F].delay(println("Hello, world")).as(ExitCode.Success)

}
