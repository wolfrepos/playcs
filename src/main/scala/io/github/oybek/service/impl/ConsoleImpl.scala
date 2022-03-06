package io.github.oybek.service.impl

import cats.implicits.*
import cats.{Monad, MonadThrow, ~>}
import io.github.oybek.common.WithMeta
import io.github.oybek.common.logger.{Context, ContextData, ContextLogger}
import io.github.oybek.common.time.Clock
import io.github.oybek.cstrike.model.Command
import io.github.oybek.cstrike.model.Command.*
import io.github.oybek.cstrike.parser.CommandParser
import io.github.oybek.database.dao.BalanceDao
import io.github.oybek.database.model.Balance
import io.github.oybek.exception.BusinessException.ZeroBalanceException
import io.github.oybek.model.Reaction.{SendText, Sleep}
import io.github.oybek.model.{ConsoleMeta, Reaction}
import io.github.oybek.service.{Console, HldsConsole, HldsConsolePoolManager}
import telegramium.bots.{ChatIntId, Markdown}

import java.time.Duration
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.{DurationInt, FiniteDuration}

class ConsoleImpl[F[_]: MonadThrow: Clock, G[_]: Monad](consolePoolManager: HldsConsolePoolManager[F],
                                                        balanceDao: BalanceDao[G],
                                                        tx: G ~> F,
                                                        log: ContextLogger[F]) extends Console[F]:

  override def handle(chatId: ChatIntId, text: String): Context[F[List[Reaction]]] =
    log.info(s"Got message $text") >> (
      CommandParser.parse(text) match
        case _: String => confusedMessage(chatId)
        case command: Command => handleCommand(chatId, command)
    )

  override def expireCheck(using contextData: ContextData): Context[F[Unit]] =
    for
      _ <- log.info("checking pool for expired consoles...")
      now <- Clock[F].instantNow
      expiredConsoles <- consolePoolManager.getConsolesWith(_.deadline.isBefore(now))
      chatIds = expiredConsoles.map(_.meta.usingBy)
      _ <- chatIds.traverse(handleFreeCommand)
      _ <- log.info(s"consoles on ports ${expiredConsoles.map(_.get.port)} is freed")
    yield ()

  private def handleCommand(chatId: ChatIntId, command: Command): Context[F[List[Reaction]]] =
    command match
      case NewCommand(map)  => handleNewCommand(chatId, map)
      case JoinCommand      => handleJoinCommand(chatId)
      case BalanceCommand   => handleBalanceCommand(chatId)
      case FreeCommand      => handleFreeCommand(chatId)
      case HelpCommand      => handleHelpCommand(chatId)
      case SayCommand(text) => handleSayCommand(chatId, text)
      case IncreaseBalanceCommand(telegramId, duration)
                            => handleIncreaseBalanceCommand(chatId, ChatIntId(telegramId), duration)
      case _                => List(SendText(chatId, "Еще не реализовано"): Reaction).pure[F]

  private def handleNewCommand(chatId: ChatIntId, map: Option[String]): Context[F[List[Reaction]]] =
    import consolePoolManager.rentConsole
    for
      Balance(_, time) <- checkAndGetBalance(chatId)
      consoleOpt <- consolePoolManager.findConsole(chatId)
      console WithMeta ConsoleMeta(pass, _, _) <- consoleOpt.fold(rentConsole(chatId, time))(_.pure[F])
      _ <- console.changeLevel(map.getOrElse("de_dust2"))
      reaction = List(
        SendText(chatId, "Сервер создан. Скопируй в консоль это"),
        Sleep(200.millis),
        sendConsole(chatId, console, pass)
      )
    yield reaction

  private def handleJoinCommand(chatId: ChatIntId): Context[F[List[Reaction]]] =
    consolePoolManager.findConsole(chatId).map {
      case None =>
        List(SendText(chatId, "Создай сервер сначала (/help)"))
      case Some(console WithMeta ConsoleMeta(password, _, _)) =>
        List(sendConsole(chatId, console, password))
    }

  private def handleBalanceCommand(chatId: ChatIntId): Context[F[List[Reaction]]] =
    tx {
      for
        balanceOpt <- balanceDao.findBy(chatId.id)
        balance <- balanceOpt.fold {
          val balance = Balance(chatId, 15.minutes)
          balanceDao.addIfNotExists(balance).as(balance)
        }(_.pure[G])
      yield List(
        SendText(chatId,
          s"""
             |Ваш баланс: ${balance.timeLeft.toSeconds/60} минут
             |Для пополнения пройдите по ссылке (1 руб = 5 мин)
             |https://www.tinkoff.ru/rm/khashimov.oybek1/Cc3Jm91036
             |В сообщении при переводе обязательно укажите следующий код
             |""".stripMargin),
        Sleep(500.millis),
        SendText(chatId, chatId.id.toString),
      )
    }

  private def handleIncreaseBalanceCommand(adminChatId: ChatIntId,
                                           chatId: ChatIntId,
                                           delta: FiniteDuration): Context[F[List[Reaction]]] =
    tx {
      for
        balanceOpt <- balanceDao.findBy(chatId.id)
        newBalance = balanceOpt.fold(0.seconds)(_.timeLeft) + delta
        _ <- balanceDao.addOrUpdate(Balance(chatId, newBalance))
      yield List(
        SendText(adminChatId, s"Увеличен баланс чата ${chatId.id} до ${newBalance}"),
        SendText(chatId, s"Ваш баланс увеличен до ${newBalance}"),
      )
    }

  private def handleFreeCommand(chatId: ChatIntId): Context[F[List[Reaction]]] =
    consolePoolManager.findConsole(chatId).flatMap {
      case Some(_ WithMeta meta) =>
        for
          _ <- consolePoolManager.freeConsole(chatId)
          now <- Clock[F].instantNow
          secondsLeft = Duration.between(now, meta.deadline).toSeconds.max(0)
          timeLeft = FiniteDuration(secondsLeft, TimeUnit.SECONDS)
          _ <- tx(balanceDao.addOrUpdate(Balance(meta.usingBy, timeLeft)))
        yield List(SendText(chatId, "Сервер освобожден"))

      case None =>
        List(SendText(chatId, "Нет созданных серверов"): Reaction).pure[F]
    }

  private def handleHelpCommand(chatId: ChatIntId): F[List[Reaction]] =
    List(SendText(chatId, helpText): Reaction).pure[F]

  private def handleSayCommand(chatId: ChatIntId, text: String): Context[F[List[Reaction]]] =
    consolePoolManager.findConsole(chatId).flatMap {
      case None =>
        List(SendText(chatId, "Создай сервер сначала (/help)")).pure[F]
      case Some(console WithMeta ConsoleMeta(password, _, _)) =>
        console.say(text).as(List.empty[Reaction])
    }

  private def sendConsole(chatId: ChatIntId, console: HldsConsole[F], password: String): SendText =
    SendText(chatId, s"`connect ${console.ip}:${console.port}; password $password`", Markdown.some)

  private def confusedMessage(chatId: ChatIntId): F[List[Reaction]] =
    List(SendText(chatId, "Не оч понял /help"): Reaction).pure[F]

  private def checkAndGetBalance(chatId: ChatIntId): Context[F[Balance]] =
    for
      balanceOpt <- tx(balanceDao.findBy(chatId.id))
      balance <- balanceOpt.fold {
        val balance = defaultBalance(chatId)
        tx(balanceDao.addIfNotExists(balance)).as(balance)
      }(_.pure[F])
      _ <- log.info(s"Chat's balance = $balance")
      _ <- MonadThrow[F].raiseWhen(
        balance.timeLeft.toSeconds <= 0
      )(ZeroBalanceException(List(SendText(chatId, "Пополните баланс /balance"): Reaction)))
    yield balance

  private def defaultBalance(chatId: ChatIntId) = Balance(chatId, 30.minutes)
