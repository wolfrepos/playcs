package io.github.oybek.organizer.service

import cats.Applicative
import cats.implicits.toFunctorOps
import cats.~>
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import io.github.oybek.organizer.model.Will
import java.time.OffsetDateTime

trait OrganizerDao[F[_]]:
  def save(intervals: Will*): F[Int]
  def selectContains(offsetDateTime: OffsetDateTime): F[List[Will]]
  def deleteContains(offsetDateTime: OffsetDateTime): F[Unit]

object OrganizerDao:
  given Write[Will] = Write[
    (Long, Long, OffsetDateTime, OffsetDateTime)
  ].contramap[Will]{ cc => 
    (cc.userId, cc.chatId, cc.start, cc.end)
  }

  def create: OrganizerDao[ConnectionIO] =
    new OrganizerDao[ConnectionIO]:
      override def save(intervals: Will*): ConnectionIO[Int] =
        Update[Will](
          """
          insert into will (user_id, chat_id, start, endd)
          values (?, ?, ?, ?)
          """
        ).updateMany(intervals)

      override def selectContains(offsetDateTime: OffsetDateTime): ConnectionIO[List[Will]] =
        sql"""
        select user_id, chat_id, start, end from interval
        where start <= $offsetDateTime and $offsetDateTime <= end
        """.query[Will].to[List]

      override def deleteContains(offsetDateTime: OffsetDateTime): ConnectionIO[Unit] =
        ???
      
