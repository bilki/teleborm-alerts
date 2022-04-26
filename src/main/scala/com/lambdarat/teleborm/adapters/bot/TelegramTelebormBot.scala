package com.lambdarat.teleborm.adapters.bot

import com.lambdarat.teleborm.domain.ports.TelebormBot

import cats._
import cats.arrow.FunctionK
import cats.effect.kernel.Async
import com.bot4s.telegram.api.declarative.Callbacks
import com.bot4s.telegram.api.declarative.Commands
import com.bot4s.telegram.cats.TelegramBot
import com.bot4s.telegram.methods.JsonRequest
import sttp.client3.SttpBackend

case class TelegramTelebormBot[F[_]: Async](backend: SttpBackend[F, _], token: String)
    extends TelegramBot[F](token, backend)
    with TelebormBot[F, JsonRequest]
    with Commands[F]
    with Callbacks[F] {

  val runner: JsonRequest ~> F = FunctionK.liftFunction[JsonRequest, F](request.apply)
}
