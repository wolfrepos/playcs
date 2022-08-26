package io.github.oybek.common

package object logger {
  type Context[T] = ContextData ?=> T

  open case class ContextData(flowId: Long)
}
