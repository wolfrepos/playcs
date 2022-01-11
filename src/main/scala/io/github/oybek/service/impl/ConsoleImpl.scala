package io.github.oybek.service.impl

import cats.{Monad, MonadThrow, ~>}
import cats.implicits.{catsSyntaxApplicativeError, catsSyntaxApplicativeId, catsSyntaxFlatMapOps, catsSyntaxOptionId, toFlatMapOps, toFunctorOps}
import io.github.oybek.common.WithMeta
import io.github.oybek.cstrike.model.Command._
import io.github.oybek.cstrike.parser.CommandParser
import io.github.oybek.database.dao.BalanceDao
import io.github.oybek.database.model.Balance
import io.github.oybek.exception.PoolManagerException.{NoFreeConsolesException, ZeroBalanceException}
import io.github.oybek.model.Reaction.{SendText, Sleep}
import io.github.oybek.model.{ConsoleMeta, Reaction}
import io.github.oybek.service.{Console, HldsConsole, HldsConsolePoolManager}
import org.typelevel.log4cats.MessageLogger
import telegramium.bots.{ChatIntId, Markdown}

import scala.concurrent.duration.DurationInt

class ConsoleImpl[F[_]: MonadThrow, G[_]: Monad](consolePoolManager: HldsConsolePoolManager[F],
                                                 balanceDao: BalanceDao[G],
                                                 tx: G ~> F,
                                                 log: MessageLogger[F]) extends Console[F] {

  def handle(chatId: ChatIntId, text: String): F[List[Reaction]] =
    CommandParser.parse(text) match {
      case Right(NewCommand(map)) => handleNewCommand(chatId, map)
      case Right(JoinCommand)     => handleJoinCommand(chatId)
      case Right(BalanceCommand)  => handleBalanceCommand(chatId)
      case Right(FreeCommand)     => handleFreeCommand(chatId)
      case Right(HelpCommand)     => List(SendText(chatId, helpText): Reaction).pure[F]
      case Right(_)               => List(SendText(chatId, "Еще не реализовано"): Reaction).pure[F]
      case Left(_)                => List(SendText(chatId, "Че? (/help)"): Reaction).pure[F]
    }

  private def handleNewCommand(chatId: ChatIntId, map: String): F[List[Reaction]] =
    consolePoolManager
      .findConsole(chatId.id)
      .flatMap(_.fold(consolePoolManager.rentConsole(chatId.id))(_.pure[F]))
      .attempt
      .flatMap {
        case Right(console WithMeta ConsoleMeta(password, _, _))  =>
          console
            .changeLevel(map)
            .as(List(
              SendText(chatId, "Сервер создан. Скопируй в консоль это"),
              Sleep(200.millis),
              sendConsole(chatId, console, password)
            ))

        case Left(ZeroBalanceException) =>
          List(SendText(chatId, "Пополните баланс /balance"): Reaction).pure[F]

        case Left(NoFreeConsolesException) =>
          List(SendText(chatId, "Не осталось свободных серверов"): Reaction).pure[F]

        case Left(th) =>
          log.info(s"Something went wrong $th") >>
          List(SendText(chatId, "Что-то пошло не так"): Reaction).pure[F]
      }

  private def handleJoinCommand(chatId: ChatIntId): F[List[Reaction]] =
    consolePoolManager.findConsole(chatId.id).map {
      case None =>
        List(SendText(chatId, "Создай сервер сначала (/help)"))

      case Some(console WithMeta ConsoleMeta(password, _, _))  =>
        List(sendConsole(chatId, console, password))
    }

  private def handleBalanceCommand(chatId: ChatIntId): F[List[Reaction]] =
    tx {
      balanceDao.findBy(chatId.id).flatMap {
        case None =>
          val balance = Balance(chatId.id, 15.minutes.toSeconds)
          balanceDao.addIfNotExists(balance).as(balance)
        case Some(balance) =>
          balance.pure[G]
      }
    } map { balance =>
      List[Reaction](
        SendText(chatId,
          s"""
             |Ваш баланс: ${balance.seconds/60} минут
             |Для пополнения пройдите по ссылке (1 руб = 2 мин)
             |https://www.tinkoff.ru/rm/khashimov.oybek1/Cc3Jm91036
             |В сообщении при переводе обязательно укажите следующий код
             |""".stripMargin),
        Sleep(500.millis),
        SendText(chatId, chatId.id.toString),
      )
    }

  private def handleFreeCommand(chatId: ChatIntId): F[List[Reaction]] =
    consolePoolManager
      .freeConsole(chatId.id)
      .as(List(SendText(chatId, "Сервер освобожден")))

  private def sendConsole(chatId: ChatIntId, console: HldsConsole[F], password: String): SendText =
    SendText(chatId, s"`connect ${console.ip}:${console.port}; password $password`", Markdown.some)
}
