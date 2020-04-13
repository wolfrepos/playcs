package io.github.oybek

import java.sql.Timestamp

import cats.effect.{Async, Concurrent, Sync, Timer}
import cats.instances.option._
import cats.syntax.all._
import fs2.Stream
import io.github.oybek.service.pool.ServerPoolAlg
import io.github.oybek.util.FileTools
import org.slf4j.{Logger, LoggerFactory}
import telegramium.bots.Update
import telegramium.bots.client.{Api, Req}

import scala.concurrent.duration._

trait BotAlg[F[_]] {
  def process(updates: Stream[F, Update]): Stream[F, Option[Req]]
}

class Bot[F[_]: Async: Timer: Concurrent](serverPool: ServerPoolAlg[F],
                                          hldsDir: String)(implicit bot: Api[F])
    extends BotAlg[F]
    with TgExtractors {

  val log: Logger = LoggerFactory.getLogger("TgGate")
  val F = Sync[F]

  import telegramium.bots._
  import telegramium.bots.client._

  private def nowAnd(minutes: FiniteDuration) =
    new Timestamp(System.currentTimeMillis() + minutes.toMillis)

  def process(updates: Stream[F, Update]): Stream[F, Option[Req]] =
    updates
      .evalTap(update => F.delay(log.info(s"got update: $update")))
      .evalMap {
        case Message(chat, Text(x)) if x.matches("/(start|help).*") =>
          val text =
            """
              |Напиши мне /go и я создам сервер CS 1.6
              |Еще меня можно добавлять в беседы
              |""".stripMargin
          sendMessageReq(chat, text)

        case Message(chat, Text(`/do`(cmd))) =>
          serverPool
            .find(chat.id)
            .flatMap(_.traverse(_.interactor.println(cmd)))
            .as(Option.empty[Req])

        case Message(chat, Text(x)) if x.startsWith("/status") =>
          serverPool.info.flatMap { x =>
            val text = x.mkString("\n")
            sendMessageReq(chat, text)
          }

        case CallbackQuery(chat, "GO!", msg) =>
          `GO!`(chat, msg.caption.getOrElse("de_dust2"), "30")

        case CallbackQuery(chat, x, msg) if x.startsWith("<< ") || x.startsWith(">> ") =>
          val map = x.drop(3).some
          menu(chat, map, msg.messageId.some)

        case Message(chat, Text(x)) if x.startsWith("/go") =>
          menu(chat)

        case Message(chat, _) =>
          sendMessageReq(chat, "Не знаю такую команду")

        case _ => F.pure(Option.empty[Req])
      }

  private def `GO!`(chat: Chat, map: String, requiredTime: String) = {
    val time = requiredTime.toInt.min(30).minutes
    serverPool.poll(chat.id, nowAnd(time), map).flatMap { resp =>
      val text =
        resp.fold(
          _ =>
            s"""
               |Не могу создать сервер - кончилась оперативка на серваке
               |Подожди пока освободится место или напиши @wolfodav
               |""".stripMargin,
          server =>
            s"""
               |Создан сервер на $map на ${time.length} минут
               |connect ${server.ip}:${server.port}; password ${server.password}
               |""".stripMargin
        )
      sendMessageReq(chat, text)
    }
  }

  private def sendMessageReq(chat: Chat, text: String) =
    F.pure(
      SendMessageReq(
        chatId = ChatLongId(chat.id),
        text = text,
        replyMarkup = None
      ).some.widen[Req]
    )

  private def menu(chat: Chat, map: Option[String] = None, msgIdOpt: Option[Int] = None) = {
    F.delay(FileTools.getPrevCurNextMap(s"$hldsDir/cstrike/maps", map)).map {
      case None =>
        SendMessageReq(chatId = ChatLongId(chat.id), text = "Нет карт").some
      case Some((prevMap, curMap, nextMap)) =>
        val replyMarkup = InlineKeyboardMarkup(
          List(
            List(
              InlineKeyboardButton(text = s"<< $prevMap", callbackData = Some(s"<< $prevMap")),
              InlineKeyboardButton(text = "GO!", callbackData = Some("GO!")),
              InlineKeyboardButton(text = s">> $nextMap", callbackData = Some(s">> $nextMap"))
            )
          )
        ).some
        val photoId = "AgACAgIAAxkBAAIGBl6oqGXfa5B_3iegvqC2VEAIOjKFAAJJrzEbB4dASQJbfTCqbhMP6fHpki4AAwEAAwIAA20AAwLAAAIZBA"
        msgIdOpt.fold(
          SendPhotoReq(
            chatId = ChatLongId(chat.id),
            photo = InputLinkFile(photoId),
            caption = curMap.some,
            replyMarkup = replyMarkup
          ).some.widen[Req]
        )(msgId =>
          EditMessageMediaReq(
            chatId = ChatLongId(chat.id).some,
            messageId = msgId.some,
            media = InputMediaPhoto(
              media = photoId,
              caption = curMap.some
            ),
            replyMarkup = replyMarkup
          ).some.widen[Req]
        )
    }
  }
}
