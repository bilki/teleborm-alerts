package com.lambdarat.teleborm.handler

import com.lambdarat.teleborm.bot.BormCommand
import com.lambdarat.teleborm.client.BormClient

import cats.effect.kernel.Async
import cats.syntax.all._

class BormCommandHandler[F[_]: Async](bormClient: BormClient[F]) {

  def handleCommand(command: BormCommand): F[String] =
    command match {
      case BormCommand.Search(words, _) =>
        for {
          result <- bormClient.search(words)
        } yield result.pretty
      case _ => "Comando no permitido".pure[F]
    }

}
