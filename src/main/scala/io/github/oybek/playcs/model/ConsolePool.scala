package io.github.oybek.playcs.model

import io.github.oybek.common.WithMeta
import io.github.oybek.console.service.ConsoleHigh

case class ConsolePool[F[_]](free: List[ConsoleHigh[F]],
                             busy: List[ConsoleHigh[F] WithMeta ConsoleMeta])
