package io.github.oybek.fakes

import telegramium.bots.ChatIntId
import telegramium.bots.User

object FakeData:
  val fakeChatId: ChatIntId = ChatIntId(0L)
  val fakeUser: User = User(fakeChatId.id, false, "joraqul")
  val adminChatId: ChatIntId = ChatIntId(123L)
  val anotherFakeChatId: ChatIntId = ChatIntId(1L)
  val fakePort = 27015
  val fakeIp = "127.0.0.1"
  val fakePassword = "4444"
