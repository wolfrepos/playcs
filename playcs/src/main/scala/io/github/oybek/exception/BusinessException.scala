package io.github.oybek.exception

import io.github.oybek.model.Reaction

sealed trait BusinessException extends Throwable:
  def reactions: List[Reaction]

object BusinessException:
  case object NoFreeConsolesException extends BusinessException:
    val reactions = List.empty[Reaction]
  case class ZeroBalanceException(reactions: List[Reaction]) extends BusinessException
  case object UnathorizedException extends BusinessException:
    val reactions = List.empty[Reaction]
