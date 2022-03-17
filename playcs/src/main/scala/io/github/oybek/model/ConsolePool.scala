package io.github.oybek.model

import io.github.oybek.common.WithMeta
import io.github.oybek.service.HldsConsole

case class ConsolePool[F[_]](free: List[HldsConsole[F]],
                             busy: List[HldsConsole[F] WithMeta ConsoleMeta])
