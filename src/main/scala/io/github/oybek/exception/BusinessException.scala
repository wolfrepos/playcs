package io.github.oybek.exception

import io.github.oybek.model.Reaction

sealed trait BusinessException extends Throwable {
  def reactions: List[Reaction]
}

object BusinessException {
  case class NoFreeConsolesException(reactions: List[Reaction]) extends BusinessException
  case class ZeroBalanceException(reactions: List[Reaction]) extends BusinessException
}
