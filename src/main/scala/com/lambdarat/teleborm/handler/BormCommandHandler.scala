package com.lambdarat.teleborm.handler

import cats.syntax.all._
import cats.effect.kernel.Async
import com.lambdarat.teleborm.bot.BormCommand

class BormCommandHandler[F[_]: Async] {
  def handleCommand(command: BormCommand): F[String] =
    command match {
      case BormCommand.Search(words, maybeFrom) =>
        val searchPrefixMessage = s"Buscando ${words.mkString(", ")}"
        maybeFrom.fold(searchPrefixMessage)(from => s"$searchPrefixMessage desde $from").pure[F]
      case _ => "Comando no permitido".pure[F]
    }

}
