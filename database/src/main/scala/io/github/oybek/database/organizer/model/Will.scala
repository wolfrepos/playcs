package io.github.oybek.organizer.model

import java.time.OffsetTime
import java.time.OffsetDateTime

final case class Will(userId: Long,
                      chatId: Long,
                      start: OffsetDateTime,
                      end: OffsetDateTime):
  override def equals(any: Any): Boolean = 
    any.isInstanceOf[Will] && {
      val will = any.asInstanceOf[Will]
      this.userId == will.userId &&
      this.chatId == will.chatId &&
      this.start.isEqual(will.start) &&
      this.end.isEqual(will.end)
    }
