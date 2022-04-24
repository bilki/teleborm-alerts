package com.lambdarat.teleborm.bot

import com.lambdarat.teleborm.domain.model._
import com.lambdarat.teleborm.domain.model.Messages._
import com.lambdarat.teleborm.calendar.CalendarAction.Ignore
import com.lambdarat.teleborm.calendar.CalendarAction.NextYears
import com.lambdarat.teleborm.calendar.CalendarAction.PrevYears
import com.lambdarat.teleborm.calendar.CalendarAction.SetDay
import com.lambdarat.teleborm.calendar.CalendarAction.SetMonth
import com.lambdarat.teleborm.calendar.CalendarAction.SetYear
import com.lambdarat.teleborm.calendar.CalendarAction.Start
import com.lambdarat.teleborm.calendar.DialogCalendar
import com.lambdarat.teleborm.handler.BormCommandHandler
import com.lambdarat.teleborm.domain.model.SearchCommandResult

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

import cats.effect.kernel.Async
import cats.syntax.all._
import com.bot4s.telegram.api.declarative._
import com.bot4s.telegram.cats.TelegramBot
import com.bot4s.telegram.methods.EditMessageReplyMarkup
import com.bot4s.telegram.models
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.syntax._
import sttp.client3.SttpBackend
import com.lambdarat.teleborm.database.UserStateStorage

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

  new IllegalArgumentException(
    "Not supported command or data in callback"
  )

  new IllegalArgumentException(
    "Callback command not supported yet"
  )

  private def runSearch[R](
      command: BormCommand.Search
  )(action: SearchCommandResult => F[R]): F[R] =
    for {
      commandResult <- commandHandler.handle(command)
      actionResult  <- action(commandResult)
    } yield actionResult

  onCallbackWithTag(DialogCalendar.tag) { implicit cb =>
    implicit val m = cb.message.get

    val attemptCalendar = for {
      data <- Async[F].fromOption(
        cb.data,
        new IllegalArgumentException("Invalid data in callback query")
      )
      cbData <- Async[F].fromOption(
        DialogCalendar.calendarCallbackDataFrom(data),
        new IllegalArgumentException("Unable to extract valid calendar callback data")
      )
      _ <- cbData.action match {
        case Ignore => unit
        case SetYear =>
          request(
            EditMessageReplyMarkup(
              chatId = cb.message.map(_.chat.chatId),
              messageId = cb.message.map(_.messageId),
              replyMarkup = DialogCalendar.getMonthMarkup(cbData.year.get).some
            )
          )
        case PrevYears =>
          val newYear = cbData.year.get - 5
          request(
            EditMessageReplyMarkup(
              chatId = cb.message.map(_.chat.chatId),
              messageId = cb.message.map(_.messageId),
              replyMarkup = DialogCalendar.getYearMarkup(newYear).some
            )
          )
        case NextYears =>
          val newYear = cbData.year.get + 5
          request(
            EditMessageReplyMarkup(
              chatId = cb.message.map(_.chat.chatId),
              messageId = cb.message.map(_.messageId),
              replyMarkup = DialogCalendar.getYearMarkup(newYear).some
            )
          )
        case Start =>
          request(
            EditMessageReplyMarkup(
              chatId = cb.message.map(_.chat.chatId),
              messageId = cb.message.map(_.messageId),
              replyMarkup = DialogCalendar.getMonthMarkup(cbData.year.get).some
            )
          )
        case SetMonth =>
          request(
            EditMessageReplyMarkup(
              chatId = cb.message.map(_.chat.chatId),
              messageId = cb.message.map(_.messageId),
              replyMarkup = DialogCalendar.getDayMarkup(cbData.year.get, cbData.month.get).some
            )
          )
        case SetDay =>
          for {
            _ <- request(
              EditMessageReplyMarkup(
                chatId = cb.message.map(_.chat.chatId),
                messageId = cb.message.map(_.messageId),
                replyMarkup = None
              )
            )
            _ <- reply(s"${LocalDate.of(cbData.year.get, cbData.month.get, cbData.day.get)}")
          } yield ()
      }
    } yield ()

    attemptCalendar.productR(ackCallback().void).handleErrorWith { case err =>
      error"Error while handling callback ${err.getMessage}" *> ackCallback().void
    }
  }

  onCommand("/calendar") { implicit msg =>
    for {
      year <- Async[F].delay(LocalDate.now.getYear)
      _    <- reply("Elige una fecha", replyMarkup = DialogCalendar.getYearMarkup(year).some)
    } yield ()
  }

  // onCallbackQuery { implicit cb =>
  //   val maybeCommand = BormCommand.extractFrom(cb.data)

  //   val attemptCommand = for {
  //     command <- Async[F].fromOption(maybeCommand, illegalCbData)
  //     _ <- command match {
  //       case search: BormCommand.Search =>
  //         runSearch(search) { commandResult =>
  //           val editMessage = EditMessageText(
  //             chatId = cb.message.map(_.chat.chatId),
  //             messageId = cb.message.map(_.messageId),
  //             parseMode = ParseMode.MarkdownV2.some,
  //             disableWebPagePreview = true.some,
  //             text = commandResult.searchResult.pretty.escapeMd,
  //             replyMarkup = commandResult.pagination.some
  //           )

  //           request(editMessage)
  //         }.void
  //       case _ => Async[F].raiseError(commandNotSupported)
  //     }
  //   } yield ()

  //   attemptCommand.handleErrorWith { case err =>
  //     error"Error while handling callback ${err.getMessage}" *> ackCallback().void
  //   }
  // }

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

  // onCommand(BormCommandType.SearchWithDate.translation) { implicit msg =>
  //   withArgs {
  //     case Seq(rawDate, word, words @ _*) =>
  //       val dateOrError =
  //         Either.catchNonFatal(DateTimeFormatter.ISO_DATE.parse(rawDate, LocalDate.from _))

  //       dateOrError.fold(
  //         _ => reply(Messages.invalidDateForSearch(rawDate)).void,
  //         { _ =>
  //           val attemptCommand = for {
  //             searchResult <- commandHandler.handleCommand(
  //               BormCommand.Search(word :: words.toList, page = 0, none[LocalDate])
  //             )
  //             _ <- replyMdV2(
  //               searchResult.escapeMd,
  //               disableWebPagePreview = true.some
  //             )
  //           } yield ()

  //           attemptCommand.onErrorContact
  //         }
  //       )
  //     case _ => reply(Messages.missingArgsForSearchWithDate).void
  //   }
  // }

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
