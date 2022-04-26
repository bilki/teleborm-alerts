package com.lambdarat.teleborm.domain.ports

import cats.~>

trait TelebormBot[F[_], W[_]] {

  def runner: W ~> F

  def send[R](method: W[R]): F[R] = runner(method)

}
