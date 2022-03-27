package io.github.oybek.service

import cats.~>
import cats.implicits.*
import io.github.oybek.organizer.model.Will
import io.github.oybek.model.Reaction
import java.time.OffsetDateTime
import io.github.oybek.organizer.dao.OrganizerDao
import cats.Monad
import telegramium.bots.Markdown2
import telegramium.bots.ChatIntId
import io.github.oybek.model.Reaction.SendText
import cats.data.NonEmptyList

trait Organizer[F[_]]:
  def save(will: NonEmptyList[Will]): F[List[Reaction]]
  def duty(offsetDateTime: OffsetDateTime): F[List[Reaction]]

object Organizer:
  def create[F[_]: Monad, G[_]](organizerDao: OrganizerDao[G],
                                transact: G ~> F): Organizer[F] =
    new Organizer[F]:
      override def save(wills: NonEmptyList[Will]): F[List[Reaction]] =
        transact(organizerDao.save(wills)).as(
          List(
            SendText(
              ChatIntId(wills.head.chatId),
              s"[${wills.head.userName}](tg://user?id=${wills.head.userId}) will play",
              Markdown2.some
            )
          )
        )

      override def duty(now: OffsetDateTime): F[List[Reaction]] =
        for
          wills <- transact(organizerDao.findBy(now))
          _ <- transact(organizerDao.deleteBefore(now))
          reaction = wills
            .groupBy(_.chatId)
            .collect {
              case (chatId, wills) if wills.distinctBy(_.userId).length >= 2 =>
                val mentionUsers = wills
                  .map(w => s"[${w.userName}](tg://user?id=${w.userId})")
                  .mkString(" ")
                SendText(ChatIntId(chatId), s"It is time to play $mentionUsers", Markdown2.some)
            }
        yield reaction.toList

  def fake[F[_]]: Organizer[F] =
    new Organizer[F]:
      override def save(wills: NonEmptyList[Will]): F[List[Reaction]] =
        ???

      override def duty(offsetDateTime: OffsetDateTime): F[List[Reaction]] =
        ???
