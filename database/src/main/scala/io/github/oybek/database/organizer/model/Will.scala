package io.github.oybek.organizer.model

import java.time.OffsetTime
import java.time.OffsetDateTime

final case class Will(userId: Long,
                      chatId: Long,
                      start: OffsetDateTime,
                      end: OffsetDateTime)
