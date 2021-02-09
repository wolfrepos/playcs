package io.github.oybek.component.pool

sealed trait ServerPoolError
case object NoFreeServerInPoolError extends ServerPoolError
case object NoSuchServerError extends ServerPoolError
