package io.github.oybek.domain

import java.sql.Timestamp

import io.github.oybek.component.console.Console

case class Server[F[_]](ip: String,
                        port: Int,
                        theMap: String,
                        password: String,
                        rentedBy: Option[Long],
                        rentedUntil: Option[Timestamp],
                        console: Console[F])
