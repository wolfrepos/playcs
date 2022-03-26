package io.github.oybek.organizer.dao

import cats.Applicative
import cats.implicits.toFunctorOps
import cats.~>
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import io.github.oybek.organizer.model.Will
import java.time.OffsetDateTime

trait OrganizerDao[F[_]]:
  def save(wills: Will*): F[Int]
  def selectContains(offsetDateTime: OffsetDateTime): F[List[Will]]
  def deleteContains(offsetDateTime: OffsetDateTime): F[Int]

object OrganizerDao:
  given Write[Will] = Write[(Long, Long, OffsetDateTime, OffsetDateTime)]
    .contramap[Will] { cc => 
      (cc.userId, cc.chatId, cc.start, cc.end)
    }

  def create: OrganizerDao[ConnectionIO] =
    new OrganizerDao[ConnectionIO]:
      override def save(wills: Will*): ConnectionIO[Int] =
        val sql = 
          """
          insert into will (user_id, chat_id, "start", "end")
          values (?, ?, ?, ?)
          """
        Update[Will](sql).updateMany(wills)

      override def selectContains(offsetDateTime: OffsetDateTime): ConnectionIO[List[Will]] =
        sql"""
        select user_id, chat_id, "start", "end" from will
        where "start" <= $offsetDateTime and $offsetDateTime <= "end"
        """.query[Will].to[List]

      override def deleteContains(offsetDateTime: OffsetDateTime): ConnectionIO[Int] =
        sql"""
        delete from will
        where "start" <= $offsetDateTime and $offsetDateTime <= "end"
        """.update.run
      
