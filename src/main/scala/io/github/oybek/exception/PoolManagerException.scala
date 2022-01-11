package io.github.oybek.exception

sealed trait PoolManagerException
object PoolManagerException {
  case object NoFreeConsolesException extends Throwable with PoolManagerException
  case object ZeroBalanceException extends Throwable with PoolManagerException
}