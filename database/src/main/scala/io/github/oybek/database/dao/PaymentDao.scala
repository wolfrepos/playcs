package io.github.oybek.database.dao

import io.github.oybek.database.model.Payment

trait PaymentDao[F[_]]:
  def add(payment: Payment): F[Unit]
