package com.lambdarat.teleborm.handler

import com.lambdarat.teleborm.bot.BormCommand
import com.lambdarat.teleborm.client.BormClient
import com.lambdarat.teleborm.model.SearchResult

import cats.effect.kernel.Async
import cats.syntax.all._

class BormCommandHandler[F[_]: Async](bormClient: BormClient[F]) {

  def handleSearch(search: BormCommand.Search): F[SearchResult] =
    bormClient.search(search.words, search.page)

  def handleCommand(command: BormCommand): F[String] =
    command match {
      case BormCommand.Search(words, page, _) =>
        for {
          result <- bormClient.search(words, page)
        } yield result.pretty
      case _ => "Comando no permitido".pure[F]
    }

}
