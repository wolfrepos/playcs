package io.github.oybek.service.pool

sealed trait ServerPoolError
case object NoFreeServerInPoolError extends ServerPoolError
case object NoSuchServerError extends ServerPoolError
