package io.github.oybek.service.uploader

import cats.effect.Timer
import fs2.Stream
import io.github.oybek.util.FileTools
import telegramium.bots.{ChatLongId, InputPartFile}
import telegramium.bots.client.{Api, SendMessageRes, SendPhotoReq, SendPhotoRes}
import cats.syntax.all._
import cats.instances.option._

import scala.concurrent.duration._

class TgImageServer[F[_]: Timer](api: Api[F]) extends ImageServerAlg[F] {

  override def uploadPhotosInDir(path: String): Stream[F, Unit] = {
    Stream
      .emits(FileTools.getListOfJPGs(new java.io.File(path)))
      .evalMap { file =>
        api
          .sendPhoto(
            SendPhotoReq(
              ChatLongId(108683062),
              InputPartFile(file),
              Some(file.getName.takeWhile(_ != '.').trim)
            )
          )
      }
      .metered(100 millis)
      .collect {
        case SendPhotoRes(_, _, Some(message)) =>
          for {
            c <- message.caption
            f <- message.photo.headOption.map(_.fileId)
          } yield (c, f)
      }
      .collect {
        case Some((name, fileId)) => (name, fileId); ()
      }
  }

  override def getPhotoIdByName(name: String): F[Option[String]] = ???
}
