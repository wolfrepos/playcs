package io.github.oybek.service.pool

import java.io.File
import java.sql.Timestamp

import cats.effect.concurrent.Ref
import cats.effect.syntax.all._
import cats.effect.{Async, Concurrent, Sync, Timer}
import cats.instances.list._
import cats.instances.option._
import cats.syntax.all._
import io.github.oybek.config.Config
import io.github.oybek.domain.{CmdStartCSDS, Server}
import io.github.oybek.service.console.Console
import io.github.oybek.util.TimeTools._
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.util.Random

class CsServerPool[F[_]: Async: Timer: Concurrent](poolRef: Ref[F, List[Server[F]]], config: Config) extends ServerPoolAlg[F] {

  private val log = LoggerFactory.getLogger("ServerPool")

  def init: F[Unit] =
    for {
      pool <- createPool
      _ <- poolRef.update(_ => pool)
      _ <- check.every(1 minute).start.void
      _ <- Sync[F].delay { log.info(s"created ${pool.length} servers") }
    } yield ()

  def info: F[List[String]] =
    poolRef.get.map {
      _.zipWithIndex.map {
        case (Server(_, _, map, _, _, Some(rentUntil), _), ind) =>
          s"$ind: $map for ${(rentUntil.getTime - System.currentTimeMillis)/60000} minutes"
        case (_, ind) =>
          s"$ind: free"
      }
    }

  def free(chatId: Long): F[Either[ServerPoolError, Unit]] =
    for {
      pool <- poolRef.get
      splitRes = split(pool, (srv: Server[F]) => srv.rentedBy.contains(chatId))
      _ <- splitRes.traverse { case (x, xs) =>
        for {
          newPool <- refresh(x).map(_::xs)
          _ <- poolRef.update(_ => newPool)
        } yield ()
      }
      res = splitRes match {
        case Some(_) => Right()
        case None => Left(NoSuchServerError)
      }
    } yield res

  def find(id: Long): F[Option[Server[F]]] =
    poolRef.get.map(_.find(_.rentedBy.contains(id)))

  def poll(chatId: Long, rentUntil: Timestamp, map: String): F[Either[ServerPoolError, Server[F]]] =
    poolRef.get.flatMap { pool =>
      split(
        pool,
        (srv: Server[F]) => srv.rentedBy.contains(chatId),
        (srv: Server[F]) => srv.rentedBy.isEmpty,
      ) match {
        case None =>
          Sync[F].delay(log.info(s"no server left in pool")) *>
            Sync[F].pure(Left(NoFreeServerInPoolError))

        case Some((x, xs)) =>
          refresh(x, map, Some(chatId), Some(rentUntil), cmd="changelevel").flatMap { srv =>
            Sync[F].delay(log.info(s"polled $srv")) *>
              poolRef.update(_ => srv::xs) *>
              Sync[F].pure(Right(srv))
          }
      }
    }

  private def split[T](l: List[T], ps: (T => Boolean)*): Option[(T, List[T])] =
    ps
      .map(p => l.span(!p(_)))
      .collectFirst {
        case (xs, y::ys) => (y, xs ++ ys)
      }

  private def check: F[Unit] = {
    for {
      now <- Sync[F].delay(new Timestamp(System.currentTimeMillis()))
      pool <- poolRef.get
      _ <- Sync[F].delay(log.info("checking expired servers..."))
      poolC <- pool.foldLeftM(List.empty[Server[F]]) {
        case (acc, cur) =>
          Sync[F].delay(log.info(s"time $now, checking $cur...")) *> (
            if (cur.rentedUntil.exists(_.before(now))) refresh(cur).map(_ :: acc)
            else Sync[F].pure(cur :: acc)
          )
      }
      _ <- poolRef.update(_ => poolC)
    } yield ()
  }

  private def refresh(srv: Server[F],
                      map: String = "de_dust2",
                      rentedBy: Option[Long] = None,
                      rentUntil: Option[Timestamp] = None,
                      password: String = randomPassword,
                      cmd: String = "map") =
    for {
      srv2 <- Sync[F].delay(srv.copy(
        theMap = map,
        rentedBy = rentedBy,
        rentedUntil = rentUntil,
        password = password
      ))
      _ <- Sync[F].delay(log.info(s"$srv -> $srv2"))
      _ <- srv2.interactor.println(s"sv_password ${srv2.password}")
      _ <- Timer[F].sleep(200 millis)
      _ <- srv2.interactor.println(s"$cmd ${srv2.theMap}")
    } yield srv2

  private def createPool: F[List[Server[F]]] =
    (0 until config.serverPoolSize).toList.traverse { i =>
      val ip = config.serverIp
      val port = 27015 + i
      Console
        .runProcess(CmdStartCSDS(new File(config.hldsDir), port))
        .map(Server(ip, port, "", "", None, None, _))
        .flatMap(refresh(_))
    }

  private def randomPassword: String = (Random.nextInt(90000) + 10000).toString
}
