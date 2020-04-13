package io.github.oybek.domain

import java.io.File

sealed trait Cmd {
  def cmd: String
  def args: Seq[(String, String)]
  def workDir: File

  def expr: String =
    (Seq(cmd) ++ args.flatMap(x => Seq(x._1, x._2))).mkString(" ")
}

case class CmdStartCSDS(workDir: File, port: Int) extends Cmd {
  def cmd: String = "./hlds_run"

  def args: Seq[(String, String)] =
    Seq(
      "-game" -> "cstrike",
      "+ip" -> "0.0.0.0",
      "+port" -> port.toString,
      "+maxplayers" -> "12",
      "+map" -> "de_dust2"
    )
}
