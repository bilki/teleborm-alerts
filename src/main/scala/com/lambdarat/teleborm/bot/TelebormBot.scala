package com.lambdarat.teleborm.bot

import com.lambdarat.teleborm.bot.Messages
import com.lambdarat.teleborm.bot.Messages._
import com.lambdarat.teleborm.database.ConversationState
import com.lambdarat.teleborm.database.UserState
import com.lambdarat.teleborm.database.UserStateStorage
import com.lambdarat.teleborm.handler.BormCommandHandler
import com.lambdarat.teleborm.model.SearchCommandResult

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

import cats.effect.kernel.Async
import cats.syntax.all._
import com.bot4s.telegram.api.declarative._
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
    commandHandler: BormCommandHandler[F],
    userStateStorage: UserStateStorage[F]
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

  onCommand(BormCommandType.Search.translation) { implicit msg =>
    for {
      now          <- Async[F].delay(LocalDateTime.now(ZoneId.of("UTC")))
      replyMessage <- replyMdV2(Messages.askForWordsSearch)
      maybeUserState = msg.from.map(user =>
        UserState(user.id, replyMessage.messageId, ConversationState.AskingSearchWords, now)
      )
      userState <- Async[F].fromOption(
        maybeUserState,
        new IllegalArgumentException("Failed to extract user id from message")
      )
      _ <-
        info"Processing message ${msg.messageId}, storing state ${userState.convState} for user ${userState.userId}"
      _ <- userStateStorage.saveUserState(userState)
    } yield ()
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

  private val isNotCommand: Filter[(models.Message, Option[models.User])] = { case (msg, _) =>
    !msg.text.exists(rawText => BormCommandType.values.map(_.translation).exists(rawText.contains))
  }

  when(onExtMessage, isNotCommand) { case (msg, _) =>
    implicit val m = msg

    val attempt = for {
      now <- Async[F].delay(LocalDateTime.now(ZoneId.of("UTC")))
      maybeUserId = msg.from.map(_.id)
      userId <- Async[F].fromOption(
        maybeUserId,
        new IllegalArgumentException("Failed to extract user id from message")
      )
      maybeUserState <- userStateStorage.getUserState(userId)
      isResponseToSearchWords = maybeUserState.exists(
        _.convState == ConversationState.AskingSearchWords
      )
      _ <- info"Processing reply for user ${userId} isResponseToSearch: ${isResponseToSearchWords}"
      _ <- Async[F].whenA(isResponseToSearchWords) {
        val maybeWords = msg.text.map(_.split(" "))

        Async[F].ifM(maybeWords.exists(_.size > 0).pure[F])(
          {
            for {
              words <- Async[F].fromOption(
                maybeWords,
                new IllegalArgumentException("Unable to extract words from message")
              )
              search = BormCommand.Search(words.toList, page = 0, none[LocalDate])
              _ <- runSearch(search) { commandResult =>
                replyMdV2(
                  commandResult.searchResult.pretty.escapeMd,
                  disableWebPagePreview = true.some,
                  replyMarkup = commandResult.pagination.some
                )
              }
              userState = UserState(userId, msg.messageId, ConversationState.Init, now)
              _ <- userStateStorage.saveUserState(userState)
            } yield ()
          },
          replyMdV2(Messages.missingArgsForSearch).void
        )
      }
    } yield ()

    attempt.onErrorContact
  }
}
