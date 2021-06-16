package io.github.oybek.playcs.model

import java.time.Instant

case class ConsoleMeta(password: String,
                       usingBy: Long,
                       deadline: Instant)