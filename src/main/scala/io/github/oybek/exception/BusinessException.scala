package io.github.oybek.exception

sealed trait BusinessException

object BusinessException {
  case object NoFreeConsolesException extends Throwable with BusinessException
  case object ZeroBalanceException extends Throwable with BusinessException
}
