package io.github.oybek.component.telegram

import cats.Parallel
import cats.effect.{Sync, Timer}
import cats.implicits._
import io.github.oybek.component.pool.ServerPoolAlg
import io.github.oybek.domain.Server

import java.sql.Timestamp
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.util.matching.Regex
import telegramium.bots.high._
import telegramium.bots.high.implicits.methodOps
import telegramium.bots.{Chat, ChatIntId, Markdown, Message, Update}

class Telegram[F[_]: Sync: Parallel: Timer](api: Api[F],
                                            serverPool: ServerPoolAlg[F])
  extends LongPollBot[F](api) {

  implicit private val apiImplicit: Api[F] = api
  val log: Logger = LoggerFactory.getLogger("telegram")

  val `/new`: Regex = "/new@playcs_bot (.*) ([0-9]+)".r
  val `/say`: Regex = "/say@playcs_bot (.*)".r

  override def onMessage(message: Message): F[Unit] = {
    Sync[F].delay(log.info(s"Got message: $message")) >> (message match {
      case Text(chat, "/how@playcs_bot") =>
        serverPool.find(chat.id).flatMap {
          case None =>
            Methods.sendMessage(
              chatId = ChatIntId(chat.id),
              text = "/new@playcs_bot de_dust2 60 - создать игру на dust2 на час"
            ).exec.void
          case Some(server) =>
            tellAboutServer(chat, server)
        }

      case Text(chat, "/map@playcs_bot") =>
        Methods.sendMessage(
          chatId = ChatIntId(chat.id),
          text = "Бля, ну карты каждый пацан должен знать: cs_assault, de_dust, de_dust2 и т. д."
        ).exec.void

      case Text(chat, "/hlp@playcs_bot") =>
        Methods.sendMessage(
          chatId = ChatIntId(chat.id),
          text =
            """
              |/new@playcs_bot de_dust2 60 - создать игру на dust2 на час
              |/say@playcs_bot hello - написать в игру 'hello'
              |/map@playcs_bot - список карт где можно чилить
              |/how@playcs_bot - напомнить как подключаться
              |/sts@playcs_bot - узнать состояние балдежа
              |/hlp@playcs_bot - вывести это сообщение
              |""".stripMargin
        ).exec.void

      case Text(chat, "/new@playcs_bot") =>
        Methods.sendMessage(
          chatId = ChatIntId(chat.id),
          text =
            """
              |Братан ты не так делаешь
              |Не надо как ебанат писать `/new@playcs_bot`
              |Вот пример:
              |`/new@playcs_bot de_dust2 60`
              |""".stripMargin,
          parseMode = Markdown.some
        ).exec.void

      case Text(chat, `/new`(map, duration)) =>
        serverPool.rent(chat.id, nowAnd(duration.toInt.minutes), map).flatMap {
          case Left(_) =>
            Methods.sendMessage(
              chatId = ChatIntId(chat.id),
              text = "Закончилось ебаное ОЗУ на машине. Напишите @wolfodav - чтобы на VDS помощнее раскошелился"
            ).exec.void

          case Right(server) =>
            tellAboutServer(chat, server, "Игра создана короче\n")
        }

      case Text(chat, "/sts@playcs_bot") =>
        serverPool.info.flatMap(x =>
          Methods.sendMessage(
            chatId = ChatIntId(chat.id),
            text = x.mkString("\n")
          ).exec.void
        )

      case Text(chat, `/say`(text)) =>
        serverPool.find(chat.id).flatMap(
          _.traverse(_.console.execute(s"say $text"))
        ).void

      case `Хуйня`(chat) =>
        Methods.sendMessage(
          chatId = ChatIntId(chat.id),
          text = "Нихуя не понял"
        ).exec.void
  })}

  private def tellAboutServer(chat: Chat, server: Server[F], created: String = "") = {
    List(
      s"${created}Скопируйте в консоль это...",
      s"`connect ${server.ip}:${server.port}; password ${server.password}`"
    ).traverse(text =>
      Methods.sendMessage(
        chatId = ChatIntId(chat.id),
        text = text,
        parseMode = Markdown.some
      ).exec.void
    ).void
  }

  private def nowAnd(minutes: FiniteDuration) =
    new Timestamp(System.currentTimeMillis() + minutes.toMillis)
}
