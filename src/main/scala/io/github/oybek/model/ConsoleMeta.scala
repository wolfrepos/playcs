package io.github.oybek.model

import java.time.Instant

case class ConsoleMeta(password: String,
                       usingBy: Long,
                       deadline: Instant)
