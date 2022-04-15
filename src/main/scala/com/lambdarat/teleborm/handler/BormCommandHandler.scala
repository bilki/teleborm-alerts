package com.lambdarat.teleborm.handler

import com.lambdarat.teleborm.bot.BormCommand
import com.lambdarat.teleborm.bot.Pagination
import com.lambdarat.teleborm.client.BormClient
import com.lambdarat.teleborm.config.BormConfig
import com.lambdarat.teleborm.model.SearchCommandResult

import cats.effect.kernel.Async
import cats.syntax.all._

class BormCommandHandler[F[_]: Async](bormClient: BormClient[F], bormConfig: BormConfig) {

  def handle(search: BormCommand.Search): F[SearchCommandResult] =
    for {
      searchResult <- bormClient.search(search.words, search.page)
      pagination = Pagination.prepareSearchButtons(
        search.words,
        search.page,
        searchResult.total,
        bormConfig.limit
      )
    } yield SearchCommandResult(searchResult, pagination)

  def handleCommand(command: BormCommand): F[String] =
    command match {
      case BormCommand.Search(words, page, _) =>
        for {
          result <- bormClient.search(words, page)
        } yield result.pretty
      case _ => "Comando no permitido".pure[F]
    }

}
