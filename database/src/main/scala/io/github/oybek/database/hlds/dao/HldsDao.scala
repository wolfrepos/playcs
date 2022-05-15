package io.github.oybek.database.hlds.dao

import doobie.*
import doobie.implicits.*
import io.github.oybek.database.hlds.model.Hlds

trait HldsDao[F[_]]:
  def add(hlds: Hlds): F[Int]
  def delete(chatId: Long): F[Int]
  def all: F[List[Hlds]]

object HldsDao:
  def create =
    new HldsDao[ConnectionIO]:
      def add(hlds: Hlds): ConnectionIO[Int] =
        addQuery(hlds).run

      def delete(chatId: Long): ConnectionIO[Int] =
        deleteQuery(chatId).run

      def all: ConnectionIO[List[Hlds]] =
        allQuery.to[List]

  def addQuery(hlds: Hlds): Update0 =
    import hlds.*
    sql"insert into server (chat_id, pass, map) values ($chatId, $pass, $map)".update

  def deleteQuery(chatId: Long): Update0 =
    sql"delete from server where chat_id = $chatId".update

  val allQuery: Query0[Hlds] =
    sql"select chat_id, pass, map from server".query[Hlds]