package com.lambdarat.teleborm.bot

import com.lambdarat.teleborm.bot.Messages
import com.lambdarat.teleborm.bot.Messages._
import com.lambdarat.teleborm.handler.BormCommandHandler
import com.lambdarat.teleborm.model.SearchCommandResult

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import cats.effect.kernel.Async
import cats.syntax.all._
import com.bot4s.telegram.api.declarative.Callbacks
import com.bot4s.telegram.api.declarative.Commands
import com.bot4s.telegram.cats.TelegramBot
import com.bot4s.telegram.methods.EditMessageText
import com.bot4s.telegram.methods.ParseMode
import com.bot4s.telegram.models
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.syntax._
import sttp.client3.SttpBackend

class TelebormBot[F[_]: Async: Logger](
    backend: SttpBackend[F, _],
    token: String,
    commandHandler: BormCommandHandler[F]
) extends TelegramBot[F](token, backend)
    with Commands[F]
    with Callbacks[F] {

  private implicit class RecoverFromCommand(commandAttempt: F[Unit])(implicit msg: models.Message) {
    def onErrorContact: F[Unit] =
      commandAttempt.recoverWith { case _ =>
        replyMdV2(
          Messages.contact.escapeMd,
          disableWebPagePreview = true.some
        ).void
      }
  }

  // Greeting/help message
  onCommand("start" | BormCommandType.Help.translation) { implicit msg =>
    replyMdV2(Messages.greeting.escapeMd).void
  }

  private val illegalCbData = new IllegalArgumentException(
    "Not supported command or data in callback"
  )

  private val commandNotSupported = new IllegalArgumentException(
    "Callback command not supported yet"
  )

  private def runSearch[R](
      command: BormCommand.Search
  )(action: SearchCommandResult => F[R]): F[R] =
    for {
      commandResult <- commandHandler.handle(command)
      actionResult  <- action(commandResult)
    } yield actionResult

  onCallbackQuery { implicit cb =>
    val maybeCommand = BormCommand.extractFrom(cb.data)

    val attemptCommand = for {
      command <- Async[F].fromOption(maybeCommand, illegalCbData)
      _ <- command match {
        case search: BormCommand.Search =>
          runSearch(search) { commandResult =>
            val editMessage = EditMessageText(
              chatId = cb.message.map(_.chat.chatId),
              messageId = cb.message.map(_.messageId),
              parseMode = ParseMode.MarkdownV2.some,
              disableWebPagePreview = true.some,
              text = commandResult.searchResult.pretty.escapeMd,
              replyMarkup = commandResult.pagination.some
            )

            request(editMessage)
          }.void
        case _ => Async[F].raiseError(commandNotSupported)
      }
    } yield ()

    attemptCommand.handleErrorWith { case err =>
      error"Error while handling callback ${err.getMessage}" *> ackCallback().void
    }
  }

  // Search by words, optionally by date
  onCommand(BormCommandType.Search.translation) { implicit msg =>
    withArgs { args =>
      if (args.isEmpty) {
        reply(Messages.missingArgsForSearch).void
      } else {
        val search = BormCommand.Search(args.toList, page = 0, none[LocalDate])

        val attemptCommand = runSearch(search) { commandResult =>
          replyMdV2(
            commandResult.searchResult.pretty.escapeMd,
            disableWebPagePreview = true.some,
            replyMarkup = commandResult.pagination.some
          )
        }

        attemptCommand.void.onErrorContact
      }
    }
  }

  onCommand(BormCommandType.SearchWithDate.translation) { implicit msg =>
    withArgs {
      case Seq(rawDate, word, words @ _*) =>
        val dateOrError =
          Either.catchNonFatal(DateTimeFormatter.ISO_DATE.parse(rawDate, LocalDate.from _))

        dateOrError.fold(
          _ => reply(Messages.invalidDateForSearch(rawDate)).void,
          { _ =>
            val attemptCommand = for {
              searchResult <- commandHandler.handleCommand(
                BormCommand.Search(word :: words.toList, page = 0, none[LocalDate])
              )
              _ <- replyMdV2(
                searchResult.escapeMd,
                disableWebPagePreview = true.some
              )
            } yield ()

            attemptCommand.onErrorContact
          }
        )
      case _ => reply(Messages.missingArgsForSearchWithDate).void
    }
  }
}
