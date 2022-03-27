package io.github.oybek.organizer.dao

import cats.Applicative
import cats.implicits.toFunctorOps
import cats.~>
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import io.github.oybek.organizer.model.Will
import java.time.OffsetDateTime
import cats.data.NonEmptyList

trait OrganizerDao[F[_]]:
  def save(wills: NonEmptyList[Will]): F[Int]
  def findBy(hour: OffsetDateTime): F[List[Will]]
  def deleteBefore(hour: OffsetDateTime): F[Int]

object OrganizerDao:
  given Write[Will] = Write[(Long, String, Long, OffsetDateTime)]
    .contramap[Will] { cc => 
      (cc.userId, cc.userName, cc.chatId, cc.hour)
    }

  def create: OrganizerDao[ConnectionIO] =
    new OrganizerDao[ConnectionIO]:
      override def save(wills: NonEmptyList[Will]): ConnectionIO[Int] =
        val sql = 
          """
          insert into will (user_id, username, chat_id, hour)
          values (?, ?, ?, ?)
          """
        Update[Will](sql).updateMany(wills)

      override def findBy(hour: OffsetDateTime): ConnectionIO[List[Will]] =
        sql"""
        select user_id, username, chat_id, hour from will
        where ${hour.minusMinutes(4)} < hour and hour < ${hour.plusMinutes(4)}
        """.query[Will].to[List]

      override def deleteBefore(hour: OffsetDateTime): ConnectionIO[Int] =
        sql"delete from will where hour < $hour".update.run
      
