package io.github.oybek.domain

import java.sql.Timestamp

import io.github.oybek.service.console.Console

case class Server[F[_]](ip: String,
                        port: Int,
                        theMap: String,
                        password: String,
                        rentedBy: Option[Long],
                        rentedUntil: Option[Timestamp],
                        interactor: Console[F])
