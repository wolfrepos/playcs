package io.github.oybek.util

import cats.syntax.all._

object FileTools {

  def getPrevCurNextMap(
    dir: String,
    map: Option[String] = None
  ): Option[(String, String, String)] = {
    val file = new java.io.File(dir)
    val maps = file.listFiles
      .filter(_.isFile)
      .map(_.getName.takeWhile(_ != '.'))
      .distinct
      .sorted
      .toVector
    val i = map.map(maps.indexOf(_)).getOrElse(0)
    if (i == -1) {
      None
    } else {
      val p = if (i == 0) maps.length - 1 else i - 1
      val n = if (i == maps.length - 1) 0 else i + 1
      (maps(p), maps(i), maps(n)).some
    }
  }

  def getListOfJPGs(dir: java.io.File): List[java.io.File] =
    dir.listFiles
      .filter(_.isFile)
      .toList
      .filter(_.getName.endsWith(".png"))
}
