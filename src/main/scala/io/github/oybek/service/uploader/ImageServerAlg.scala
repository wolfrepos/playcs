package io.github.oybek.service.uploader

trait ImageServerAlg[F[_]] {

  def uploadPhotosInDir(path: String): fs2.Stream[F, Unit]
  def getPhotoIdByName(name: String): F[Option[String]]
}
