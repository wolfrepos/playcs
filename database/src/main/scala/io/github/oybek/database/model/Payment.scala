package io.github.oybek.database.model

import java.time.OffsetDateTime

case class Payment(id: Long,
                   rubles: Long,
                   telegramId: Long,
                   chargeTime: OffsetDateTime)
